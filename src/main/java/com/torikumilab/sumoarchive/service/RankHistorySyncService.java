package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.client.SumoApiClient;
import com.torikumilab.sumoarchive.domain.dto.sumoapi.SumoApiRankDTO;
import com.torikumilab.sumoarchive.domain.dto.sumoapi.SumoApiRikishiDTO;
import com.torikumilab.sumoarchive.domain.entity.BanzukeEntity;
import com.torikumilab.sumoarchive.domain.entity.BashoEntity;
import com.torikumilab.sumoarchive.domain.entity.RikishiEntity;
import com.torikumilab.sumoarchive.domain.entity.constant.Division;
import com.torikumilab.sumoarchive.repository.BanzukeRepository;
import com.torikumilab.sumoarchive.repository.BashoRepository;
import com.torikumilab.sumoarchive.repository.RikishiRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 반즈케 변동(BanzukeChangeService)에 필요한 sumo-api 정보를 채운다.
 * <ol>
 *   <li>그 바쇼 반즈케 선수 중 계급 이력을 아직 안 받은 선수 → /api/ranks로 처음 반즈케·세키토리·마쿠우치 바쇼.
 *       과거 이력은 바뀌지 않으니 선수당 한 번만 받는다 (그 뒤는 우리 반즈케로 보충)</li>
 *   <li>직전 바쇼엔 있었는데 이번 반즈케에 없는 선수 중 은퇴일이 없는 선수 → /api/rikishi로 은퇴(intai) 확인.
 *       현역 로스터 동기화는 은퇴자를 받지 않아서, 그대로 두면 은퇴한 선수도 현역으로 남는다</li>
 * </ol>
 * 바쇼 일괄 임포트 마지막 단계에서 돌고, 처음 한 번은 관리자 「반즈케 변동 이력 채우기」로 전 바쇼를 채운다.
 * 외부 호출이 길어서 트랜잭션은 선수 한 명 저장마다 짧게 연다.
 */
@Service
@RequiredArgsConstructor
public class RankHistorySyncService {

	private static final Logger log = LoggerFactory.getLogger(RankHistorySyncService.class);

	/** 결과 화면이 길어지지 않게 경고는 이만큼만 담는다 (전체 수는 failures) */
	private static final int MAX_WARNINGS = 30;

	private static final Map<String, Division> DIVISION_BY_RANK_WORD = Map.ofEntries(
			Map.entry("Yokozuna", Division.Makuuchi), Map.entry("Ozeki", Division.Makuuchi),
			Map.entry("Sekiwake", Division.Makuuchi), Map.entry("Komusubi", Division.Makuuchi),
			Map.entry("Maegashira", Division.Makuuchi), Map.entry("Juryo", Division.Juryo),
			Map.entry("Makushita", Division.Makushita), Map.entry("Sandanme", Division.Sandanme),
			Map.entry("Jonidan", Division.Jonidan), Map.entry("Jonokuchi", Division.Jonokuchi));

	private final SumoApiClient sumoApiClient;
	private final BashoRepository bashoRepository;
	private final BanzukeRepository banzukeRepository;
	private final RikishiRepository rikishiRepository;
	private final TransactionTemplate transactionTemplate;

	public record SyncResult(int historyFetched, int retiredFound, int failures, List<String> warnings) {}

	/** 처음 반즈케 / 세키토리 / 마쿠우치 바쇼 (YYYYMM, 없으면 null) */
	record FirstBasho(String banzuke, String sekitori, String makuuchi) {}

	@CacheEvict(value = "banzuke", allEntries = true)
	public SyncResult syncForBasho(Integer bashoId) {
		Counter c = new Counter();
		sync(bashoId, bashoRepository.findAllWithBanzukeOrderByStartDateDesc(), new HashSet<>(), c);
		return c.result();
	}

	/** 반즈케가 있는 모든 바쇼 (처음 한 번 채우기용 - 선수 천여 명이라 수 분 걸린다). */
	@CacheEvict(value = "banzuke", allEntries = true)
	public SyncResult syncAll() {
		List<BashoEntity> bashos = bashoRepository.findAllWithBanzukeOrderByStartDateDesc();
		Counter c = new Counter();
		Set<Integer> done = new HashSet<>();
		for (BashoEntity b : bashos) {
			sync(b.getId(), bashos, done, c);
		}
		return c.result();
	}

	/** done: 이번 실행에서 이미 확인한 리키시 id (전 바쇼를 돌 때 같은 선수를 또 조회하지 않게) */
	private void sync(Integer bashoId, List<BashoEntity> bashos, Set<Integer> done, Counter c) {
		BashoEntity prev = null;
		for (int i = 0; i + 1 < bashos.size(); i++) {
			if (bashos.get(i).getId().equals(bashoId)) {
				prev = bashos.get(i + 1);
				break;
			}
		}

		List<BanzukeEntity> now = banzukeRepository.findByBashoEntityIdFetchRikishi(bashoId);
		for (BanzukeEntity b : now) {
			RikishiEntity r = b.getRikishiEntity();
			if (r.isRankHistorySynced() || r.getExternalApiId() == null || !done.add(r.getId())) {
				continue;
			}
			try {
				FirstBasho first = firstBasho(sumoApiClient.getRankHistory(r.getExternalApiId()));
				transactionTemplate.executeWithoutResult(s -> rikishiRepository.findById(r.getId())
						.ifPresent(e -> e.applyRankHistory(first.banzuke(), first.sekitori(), first.makuuchi())));
				c.historyFetched++;
			} catch (RuntimeException e) {
				c.fail(r.getShikonaEn() + " 계급 이력 조회 실패: " + e.getMessage());
			}
		}

		if (prev == null) {
			return;
		}
		Set<Integer> nowIds = now.stream().map(b -> b.getRikishiEntity().getId()).collect(Collectors.toSet());
		for (BanzukeEntity b : banzukeRepository.findByBashoEntityIdFetchRikishi(prev.getId())) {
			RikishiEntity r = b.getRikishiEntity();
			if (nowIds.contains(r.getId()) || r.getRetiredDate() != null || r.getExternalApiId() == null
					|| !done.add(-r.getId())) { // 은퇴 확인은 이력과 따로 한 번 (음수 키)
				continue;
			}
			SumoApiRikishiDTO api = sumoApiClient.getRikishi(r.getExternalApiId());
			LocalDate retired = api == null ? null : parseDate(api.intai());
			if (retired != null) {
				transactionTemplate.executeWithoutResult(s -> rikishiRepository.findById(r.getId())
						.ifPresent(e -> e.markRetired(retired)));
				c.retiredFound++;
			}
		}
	}

	/** 마에즈모·반즈케 외 기록은 빼고 계급 이력에서 처음 오른 바쇼들. bashoId가 YYYYMM이라 문자열 비교로 순서가 맞다. */
	static FirstBasho firstBasho(List<SumoApiRankDTO> history) {
		String banzuke = null;
		String sekitori = null;
		String makuuchi = null;
		for (SumoApiRankDTO h : history) {
			Division d = divisionOf(h.rank());
			if (d == null || h.bashoId() == null || h.bashoId().length() != 6) {
				continue;
			}
			banzuke = earlier(banzuke, h.bashoId());
			if (d == Division.Makuuchi || d == Division.Juryo) {
				sekitori = earlier(sekitori, h.bashoId());
			}
			if (d == Division.Makuuchi) {
				makuuchi = earlier(makuuchi, h.bashoId());
			}
		}
		return new FirstBasho(banzuke, sekitori, makuuchi);
	}

	/** "Maegashira 5 East" → Makuuchi. "Mae-zumo", "Banzuke-gai" 등 반즈케 밖이면 null. */
	static Division divisionOf(String rank) {
		if (rank == null || rank.isBlank()) {
			return null;
		}
		return DIVISION_BY_RANK_WORD.get(rank.strip().split("\\s+", 2)[0]);
	}

	private static String earlier(String a, String b) {
		return a == null || b.compareTo(a) < 0 ? b : a;
	}

	private static LocalDate parseDate(String iso) {
		if (iso == null || iso.length() < 10) {
			return null;
		}
		try {
			return LocalDate.parse(iso.substring(0, 10));
		} catch (RuntimeException e) {
			return null;
		}
	}

	private static final class Counter {
		int historyFetched;
		int retiredFound;
		int failures;
		final List<String> warnings = new ArrayList<>();

		void fail(String message) {
			failures++;
			if (warnings.size() < MAX_WARNINGS) {
				warnings.add(message);
			}
		}

		SyncResult result() {
			log.info("[RankHistorySync] 이력 {}명, 은퇴 확인 {}명, 실패 {}건", historyFetched, retiredFound, failures);
			return new SyncResult(historyFetched, retiredFound, failures, List.copyOf(warnings));
		}
	}
}
