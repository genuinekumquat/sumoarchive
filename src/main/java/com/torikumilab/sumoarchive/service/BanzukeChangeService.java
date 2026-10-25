package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.domain.dto.BanzukeChangesDTO;
import com.torikumilab.sumoarchive.domain.dto.BanzukeChangesDTO.Entry;
import com.torikumilab.sumoarchive.domain.entity.BanzukeEntity;
import com.torikumilab.sumoarchive.domain.entity.BashoEntity;
import com.torikumilab.sumoarchive.domain.entity.HeyaEntity;
import com.torikumilab.sumoarchive.domain.entity.RikishiEntity;
import com.torikumilab.sumoarchive.domain.entity.constant.Division;
import com.torikumilab.sumoarchive.repository.BanzukeRepository;
import com.torikumilab.sumoarchive.repository.BashoRepository;
import com.torikumilab.sumoarchive.util.RankDisplayUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 메인 반즈케 탭 「이번 반즈케 변동」 - 한 바쇼 반즈케를 직전 바쇼(반즈케가 있는 바로 앞 바쇼)와 비교한다.
 *
 * <ul>
 *   <li>신입막: 이번에 처음 마쿠우치. 재입막: 전에 마쿠우치였는데 직전 바쇼엔 마쿠우치가 아니었음</li>
 *   <li>신주료: 이번에 처음 세키토리(주료). 재주료: 전에 세키토리였는데 직전 바쇼엔 마쿠시타 이하 - 마쿠우치에서 내려온 건 아님</li>
 *   <li>신제자: 이번에 처음 반즈케에 오름 (마에즈모 다음 바쇼의 조노구치, 마쿠시타 츠케다시 포함)</li>
 *   <li>은퇴 / 반즈케 제외: 직전 반즈케엔 있는데 이번엔 없음. sumo-api 은퇴일이 있으면 은퇴</li>
 * </ul>
 *
 * 「처음」은 우리 반즈케(2019년~)와 sumo-api 전체 계급 이력(rikishi.first_*_basho, RankHistorySyncService)
 * 중 이른 쪽으로 본다 - 2019년 전에 마쿠우치였던 베테랑이 돌아와도 재입막으로 나오게.
 * 결과는 "banzuke" 캐시에 같이 담아, 반즈케·리키시를 바꾸는 기존 캐시 비우기를 그대로 탄다.
 */
@Service
@RequiredArgsConstructor
public class BanzukeChangeService {

	private static final DateTimeFormatter YYYYMM = DateTimeFormatter.ofPattern("yyyyMM");

	private static final Set<Division> SEKITORI = Set.of(Division.Makuuchi, Division.Juryo);

	/** 표시 순서: 디비전 → 계급 → 순번 → 동서 */
	private static final Comparator<BanzukeEntity> RANK_ORDER = Comparator
			.comparing(BanzukeEntity::getDivision)
			.thenComparing(BanzukeEntity::getRankName)
			.thenComparing(b -> b.getRankValue() == null ? 0 : b.getRankValue())
			.thenComparing(BanzukeEntity::getSide);

	private final BashoRepository bashoRepository;
	private final BanzukeRepository banzukeRepository;

	/** 처음 반즈케 / 세키토리 / 마쿠우치에 오른 바쇼. 없으면 null. */
	record Career(YearMonth firstBanzuke, YearMonth firstSekitori, YearMonth firstMakuuchi) {}

	enum SekitoriChange { NEW_MAKUUCHI, RETURN_MAKUUCHI, NEW_JURYO, RETURN_JURYO }

	/**
	 * bashoId가 null이면 반즈케가 있는 최신 바쇼. 직전 바쇼가 없으면(가장 오래된 바쇼) 비교할 수 없어서 null.
	 */
	@Cacheable(value = "banzuke", key = "'changes-' + (#bashoId != null ? #bashoId : 'latest')")
	@Transactional(readOnly = true)
	public BanzukeChangesDTO getChanges(Integer bashoId) {
		List<BashoEntity> bashos = bashoRepository.findAllWithBanzukeOrderByStartDateDesc();
		int idx = 0;
		if (bashoId != null) {
			idx = -1;
			for (int i = 0; i < bashos.size(); i++) {
				if (bashos.get(i).getId().equals(bashoId)) {
					idx = i;
					break;
				}
			}
		}
		if (idx < 0 || idx + 1 >= bashos.size()) {
			return null;
		}
		BashoEntity basho = bashos.get(idx);
		BashoEntity prev = bashos.get(idx + 1);
		YearMonth cur = yearMonth(basho);

		List<BanzukeEntity> now = banzukeRepository.findByBashoFetchRikishiHeya(basho.getId());
		List<BanzukeEntity> before = banzukeRepository.findByBashoFetchRikishiHeya(prev.getId());
		Map<Integer, Division> prevDivision = before.stream()
				.collect(Collectors.toMap(b -> b.getRikishiEntity().getId(), BanzukeEntity::getDivision, (a, b) -> a));
		Map<Integer, Map<Division, LocalDate>> firstInDb = firstStartDates(now);

		List<Entry> newMakuuchi = new ArrayList<>();
		List<Entry> returnMakuuchi = new ArrayList<>();
		List<Entry> newJuryo = new ArrayList<>();
		List<Entry> returnJuryo = new ArrayList<>();
		List<Entry> newRecruits = new ArrayList<>();
		for (BanzukeEntity b : now.stream().sorted(RANK_ORDER).toList()) {
			RikishiEntity r = b.getRikishiEntity();
			Career career = career(r, firstInDb.getOrDefault(r.getId(), Map.of()));
			SekitoriChange change = sekitoriChange(b.getDivision(), prevDivision.get(r.getId()), career, cur);
			if (change != null) {
				switch (change) {
					case NEW_MAKUUCHI -> newMakuuchi.add(entry(b));
					case RETURN_MAKUUCHI -> returnMakuuchi.add(entry(b));
					case NEW_JURYO -> newJuryo.add(entry(b));
					case RETURN_JURYO -> returnJuryo.add(entry(b));
				}
			}
			if (cur.equals(career.firstBanzuke())) {
				newRecruits.add(entry(b));
			}
		}

		Set<Integer> nowIds = now.stream().map(b -> b.getRikishiEntity().getId()).collect(Collectors.toSet());
		List<Entry> retired = new ArrayList<>();
		List<Entry> dropped = new ArrayList<>();
		for (BanzukeEntity b : before.stream().sorted(RANK_ORDER).toList()) {
			if (nowIds.contains(b.getRikishiEntity().getId())) {
				continue;
			}
			(b.getRikishiEntity().getRetiredDate() != null ? retired : dropped).add(entry(b));
		}

		return new BanzukeChangesDTO(
				prev.getBashoYear(), prev.getBashoMonth().getDisplayNameKr(), prev.getBashoMonth().getDisplayNameJp(),
				newMakuuchi, returnMakuuchi, newJuryo, returnJuryo, newRecruits, retired, dropped);
	}

	/** 마쿠우치·주료 선수의 변동. 해당 없으면 null. career는 이번 바쇼를 포함한 기록이라 first*는 cur 이하. */
	static SekitoriChange sekitoriChange(Division division, Division prevDivision, Career career, YearMonth cur) {
		if (division == Division.Makuuchi) {
			if (cur.equals(career.firstMakuuchi())) {
				return SekitoriChange.NEW_MAKUUCHI;
			}
			return prevDivision != Division.Makuuchi ? SekitoriChange.RETURN_MAKUUCHI : null;
		}
		if (division == Division.Juryo) {
			if (cur.equals(career.firstSekitori())) {
				return SekitoriChange.NEW_JURYO;
			}
			// Set.of는 contains(null)에 NPE - 직전 반즈케에 없던 선수(휴장 등)는 null로 온다
			return prevDivision == null || !SEKITORI.contains(prevDivision) ? SekitoriChange.RETURN_JURYO : null;
		}
		return null;
	}

	/** sumo-api 이력(이력을 받았으면)과 우리 반즈케의 디비전별 첫 바쇼 중 이른 쪽. */
	static Career career(RikishiEntity r, Map<Division, LocalDate> firstInDb) {
		YearMonth dbAny = null;
		YearMonth dbSekitori = null;
		YearMonth dbMakuuchi = null;
		for (Map.Entry<Division, LocalDate> e : firstInDb.entrySet()) {
			YearMonth ym = YearMonth.from(e.getValue());
			dbAny = min(dbAny, ym);
			if (SEKITORI.contains(e.getKey())) {
				dbSekitori = min(dbSekitori, ym);
			}
			if (e.getKey() == Division.Makuuchi) {
				dbMakuuchi = min(dbMakuuchi, ym);
			}
		}
		return new Career(
				min(dbAny, parse(r.getFirstBanzukeBasho())),
				min(dbSekitori, parse(r.getFirstJuryoBasho())),
				min(dbMakuuchi, parse(r.getFirstMakuuchiBasho())));
	}

	private Map<Integer, Map<Division, LocalDate>> firstStartDates(List<BanzukeEntity> rows) {
		Map<Integer, Map<Division, LocalDate>> result = new HashMap<>();
		if (rows.isEmpty()) {
			return result;
		}
		List<Integer> ids = rows.stream().map(b -> b.getRikishiEntity().getId()).toList();
		for (Object[] row : banzukeRepository.findFirstStartDateByDivision(ids)) {
			result.computeIfAbsent((Integer) row[0], k -> new EnumMap<>(Division.class))
					.put((Division) row[1], (LocalDate) row[2]);
		}
		return result;
	}

	private static Entry entry(BanzukeEntity b) {
		RikishiEntity r = b.getRikishiEntity();
		HeyaEntity h = r.getHeyaEntity();
		return new Entry(
				r.getId(), r.getShikonaKr(), r.getShikonaJp(), r.getShikonaEn(),
				RankDisplayUtil.rankDisplayKorean(b.getRankName(), b.getRankValue()),
				RankDisplayUtil.rankDisplay(b.getRankName(), b.getRankValue()),
				h != null ? h.getNameKr() : null, h != null ? h.getNameJp() : null);
	}

	private static YearMonth yearMonth(BashoEntity b) {
		return YearMonth.of(b.getBashoYear(), b.getBashoMonth().getMonthValue());
	}

	static YearMonth parse(String yyyymm) {
		if (yyyymm == null || yyyymm.length() != 6) {
			return null;
		}
		try {
			return YearMonth.parse(yyyymm, YYYYMM);
		} catch (RuntimeException e) {
			return null;
		}
	}

	private static YearMonth min(YearMonth a, YearMonth b) {
		if (a == null) {
			return b;
		}
		if (b == null) {
			return a;
		}
		return a.isBefore(b) ? a : b;
	}
}
