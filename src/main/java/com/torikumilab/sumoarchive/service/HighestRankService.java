package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.domain.entity.BanzukeEntity;
import com.torikumilab.sumoarchive.domain.entity.BashoEntity;
import com.torikumilab.sumoarchive.domain.entity.RikishiEntity;
import com.torikumilab.sumoarchive.domain.entity.constant.RankName;
import com.torikumilab.sumoarchive.repository.BanzukeRepository;
import com.torikumilab.sumoarchive.repository.BashoRepository;
import com.torikumilab.sumoarchive.util.RankDisplayUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 프로필·검색·즐겨찾기에 보여줄 최고위.
 *
 * <p>관리자가 직접 입력한 {@code highest_rank}가 있으면 그 값, 없으면 적재된 반즈케 중 가장 높은 지위
 * (같은 지위면 처음 오른 바쇼)로 계산한다. 반즈케는 2019년부터만 있어서, 그 전에 데뷔한 리키시의 계산값은
 * 실제 최고위보다 낮을 수 있다 → {@code partial}로 표시한다. 단 sumo-api 전체 계급 이력과 대조해 같다고 확인된
 * 리키시({@code highest_rank_verified})와 요코즈나(더 높은 지위가 없음)는 빼고, 실제가 더 높았던 리키시는
 * {@code highest_rank}에 직접 입력해 둔다.</p>
 */
@Service
@RequiredArgsConstructor
public class HighestRankService {

	/** 높은 지위가 먼저. 번호 없는 지위(요코즈나~코무스비)는 번호를 보지 않고, 같으면 먼저 오른 바쇼. */
	static final Comparator<BanzukeEntity> HIGHEST_FIRST = Comparator
			.comparing((BanzukeEntity b) -> b.getRankName().ordinal())
			.thenComparing(b -> isNumbered(b.getRankName()) ? b.getRankValue() : null,
					Comparator.nullsLast(Comparator.naturalOrder()))
			.thenComparing(b -> b.getBashoEntity().getStartDate());

	private final BanzukeRepository banzukeRepository;
	private final BashoRepository bashoRepository;

	/**
	 * @param rank      화면 표기 ("마에가시라3" / "前頭3枚目", 직접 입력값은 "오제키" / "大関")
	 * @param since     처음 오른 바쇼 ("2024년 5월 나츠바쇼" / "2024年05月場所"). 직접 입력값이면 null
	 * @param partial   2019년 전 데뷔(또는 데뷔일 모름)라 적재된 기록만으로 계산했고, 실제와 같은지 확인되지 않은 값
	 */
	public record HighestRank(String rank, String rankJp, String since, String sinceJp, boolean partial) {
	}

	/**
	 * 계산에 필요한 리키시 정보. 검색 결과처럼 데뷔 연도만 있으면 그해 1월로 둔다
	 * (기록 시작이 1월 바쇼라 연도만으로도 판단이 같다). 검색·즐겨찾기 카드는 partial을 쓰지 않아 verified를 몰라도 된다.
	 */
	public record Subject(Integer rikishiId, String manualHighestRank, YearMonth debut, boolean verified) {
		public static Subject of(RikishiEntity r) {
			return new Subject(r.getId(), r.getHighestRank(),
					r.getDebutDate() != null ? YearMonth.from(r.getDebutDate()) : null, r.isHighestRankVerified());
		}
	}

	/** 프로필용: 이미 불러온 그 리키시의 반즈케 전체(바쇼 fetch)로 계산. */
	public HighestRank resolve(RikishiEntity r, List<BanzukeEntity> banzukeHistory) {
		return resolve(Subject.of(r), banzukeHistory, dataStart());
	}

	/** 검색·즐겨찾기용: 여러 리키시를 한 번에 (반즈케 1쿼리). 결과가 없는 리키시는 맵에 없다. */
	public Map<Integer, HighestRank> resolveAll(Collection<Subject> subjects) {
		if (subjects.isEmpty()) {
			return Map.of();
		}
		Map<Integer, List<BanzukeEntity>> byRikishi = banzukeRepository
				.findByRikishiIdsFetchBasho(subjects.stream().map(Subject::rikishiId).toList())
				.stream()
				.collect(Collectors.groupingBy(b -> b.getRikishiEntity().getId()));
		LocalDate dataStart = dataStart();
		Map<Integer, HighestRank> result = new HashMap<>();
		for (Subject s : subjects) {
			HighestRank h = resolve(s, byRikishi.getOrDefault(s.rikishiId(), List.of()), dataStart);
			if (h != null) {
				result.put(s.rikishiId(), h);
			}
		}
		return result;
	}

	static HighestRank resolve(Subject s, List<BanzukeEntity> banzukeHistory, LocalDate dataStart) {
		String manual = s.manualHighestRank();
		if (manual != null && !manual.isBlank()) {
			return new HighestRank(RankDisplayUtil.toKorean(manual), RankDisplayUtil.toJapanese(manual), null, null, false);
		}
		BanzukeEntity best = banzukeHistory.stream().min(HIGHEST_FIRST).orElse(null);
		if (best == null) {
			return null;
		}
		BashoEntity basho = best.getBashoEntity();
		boolean partial = !s.verified() && best.getRankName() != RankName.Yokozuna
				&& (dataStart == null || s.debut() == null || s.debut().isBefore(YearMonth.from(dataStart)));
		return new HighestRank(
				RankDisplayUtil.rankDisplayKorean(best.getRankName(), best.getRankValue()),
				RankDisplayUtil.rankDisplay(best.getRankName(), best.getRankValue()),
				basho.getBashoYear() + "년 " + basho.getBashoMonth().getMonthValue() + "월 " + basho.getBashoMonth().getDisplayNameKr(),
				basho.getBashoYear() + "年" + String.format("%02d", basho.getBashoMonth().getMonthValue()) + "月場所",
				partial);
	}

	/** 반즈케가 적재된 가장 이른 바쇼의 시작일 (2019년 1월). 반즈케가 하나도 없으면 null. */
	private LocalDate dataStart() {
		return bashoRepository.findEarliestStartDateWithBanzuke();
	}

	private static boolean isNumbered(RankName rankName) {
		return rankName.ordinal() >= RankName.Maegashira.ordinal();
	}
}
