package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.domain.dto.CompareDTO;
import com.torikumilab.sumoarchive.domain.entity.BanzukeEntity;
import com.torikumilab.sumoarchive.domain.entity.BashoEntity;
import com.torikumilab.sumoarchive.domain.entity.RikishiEntity;
import com.torikumilab.sumoarchive.domain.entity.TorikumiEntity;
import com.torikumilab.sumoarchive.domain.entity.constant.KimariteCategory;
import com.torikumilab.sumoarchive.domain.entity.constant.ResultType;
import com.torikumilab.sumoarchive.repository.BanzukeRepository;
import com.torikumilab.sumoarchive.repository.BashoRepository;
import com.torikumilab.sumoarchive.repository.RikishiRepository;
import com.torikumilab.sumoarchive.repository.TorikumiRepository;
import com.torikumilab.sumoarchive.util.KimariteDisplayUtil;
import com.torikumilab.sumoarchive.util.RankDisplayUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 두 리키시 비교(/compare) 화면 조립. 맞대결 전적·결정기술, 최근 6바쇼 성적을 나란히 보여준다.
 * 두 선수의 통산 경기(결정전 제외)를 한 번씩만 읽어 맞대결과 바쇼별 성적을 모두 메모리에서 뽑는다.
 */
@Service
@RequiredArgsConstructor
public class CompareService {

	/** 진행 중인 바쇼와 별개로 보여줄 끝난 바쇼 수 */
	static final int RECENT_BASHO_COUNT = 6;
	/** 종료일이 없는 바쇼는 첫날부터 15일로 본다 (센슈라쿠 = 시작일 + 14일) */
	private static final int BASHO_LAST_DAY_OFFSET = 14;

	private final RikishiRepository rikishiRepository;
	private final TorikumiRepository torikumiRepository;
	private final BanzukeRepository banzukeRepository;
	private final BashoRepository bashoRepository;

	public CompareDTO.Side getSide(Integer rikishiId) {
		RikishiEntity r = find(rikishiId);
		List<BanzukeEntity> banzuke = banzukeRepository.findByRikishiIdOrderByBashoDesc(rikishiId);
		return toSide(r, banzuke.isEmpty() ? null : banzuke.get(0));
	}

	public CompareDTO compare(Integer aId, Integer bId, LocalDate today) {
		RikishiEntity a = find(aId);
		RikishiEntity b = find(bId);
		List<BanzukeEntity> aBanzuke = banzukeRepository.findByRikishiIdOrderByBashoDesc(aId);
		List<BanzukeEntity> bBanzuke = banzukeRepository.findByRikishiIdOrderByBashoDesc(bId);
		List<TorikumiEntity> aMatches = torikumiRepository.findAllRegularByRikishi(aId);
		List<TorikumiEntity> bMatches = torikumiRepository.findAllRegularByRikishi(bId);

		// 맞대결: A의 경기 중 상대가 B인 것 (리포지토리가 최신 바쇼부터 내려준다)
		List<TorikumiEntity> h2h = aMatches.stream()
				.filter(t -> involves(t, bId))
				.toList();
		long aWins = h2h.stream().filter(t -> isWinner(t, aId)).count();
		long bWins = h2h.stream().filter(t -> isWinner(t, bId)).count();
		long fusen = h2h.stream().filter(t -> t.getResultType() == ResultType.FUZEN).count();

		List<TorikumiEntity> aWonByTechnique = wonByTechnique(h2h, aId);
		List<TorikumiEntity> bWonByTechnique = wonByTechnique(h2h, bId);

		LocalDate earliest = bashoRepository.findEarliestStartDateWithBanzuke();

		return new CompareDTO(
				toSide(a, aBanzuke.isEmpty() ? null : aBanzuke.get(0)),
				toSide(b, bBanzuke.isEmpty() ? null : bBanzuke.get(0)),
				aWins,
				bWins,
				fusen,
				h2h.stream().map(t -> toBout(t, aId)).toList(),
				kimariteCounts(aWonByTechnique),
				kimariteCounts(bWonByTechnique),
				categoryCounts(aWonByTechnique),
				categoryCounts(bWonByTechnique),
				bashoRows(today, aId, bId, aBanzuke, bBanzuke, aMatches, bMatches),
				earliest != null ? earliest.getYear() : null);
	}

	/**
	 * 보여줄 바쇼: 진행 중인 바쇼(첫날~센슈라쿠 사이, 있으면 맨 위) + 그 전에 끝난 바쇼 최근 6개.
	 * 반즈케 발표 후 개막 전인 바쇼는 아직 열리지 않았으니 넣지 않는다.
	 */
	static List<BashoEntity> pickBasho(List<BashoEntity> allNewestFirst, LocalDate today) {
		List<BashoEntity> result = new ArrayList<>();
		int finished = 0;
		for (BashoEntity basho : allNewestFirst) {
			LocalDate start = basho.getStartDate();
			if (start == null || start.isAfter(today)) {
				continue;
			}
			if (isInProgress(basho, today)) {
				if (result.isEmpty()) {
					result.add(basho);
				}
				continue;
			}
			result.add(basho);
			if (++finished == RECENT_BASHO_COUNT) {
				break;
			}
		}
		return result;
	}

	private static boolean isInProgress(BashoEntity basho, LocalDate today) {
		LocalDate start = basho.getStartDate();
		if (start == null || start.isAfter(today)) {
			return false;
		}
		LocalDate end = basho.getEndDate() != null ? basho.getEndDate() : start.plusDays(BASHO_LAST_DAY_OFFSET);
		return !end.isBefore(today);
	}

	private List<CompareDTO.BashoRow> bashoRows(LocalDate today, Integer aId, Integer bId,
												 List<BanzukeEntity> aBanzuke, List<BanzukeEntity> bBanzuke,
												 List<TorikumiEntity> aMatches, List<TorikumiEntity> bMatches) {
		Map<Integer, BanzukeEntity> aByBasho = byBasho(aBanzuke);
		Map<Integer, BanzukeEntity> bByBasho = byBasho(bBanzuke);
		Map<Integer, List<TorikumiEntity>> aMatchesByBasho = aMatches.stream()
				.collect(Collectors.groupingBy(t -> t.getBashoEntity().getId()));
		Map<Integer, List<TorikumiEntity>> bMatchesByBasho = bMatches.stream()
				.collect(Collectors.groupingBy(t -> t.getBashoEntity().getId()));


		return pickBasho(bashoRepository.findAllByOrderByStartDateDesc(), today).stream()
				.map(basho -> {
					boolean inProgress = isInProgress(basho, today);
					Integer id = basho.getId();
					return new CompareDTO.BashoRow(
							bashoLabelKr(basho),
							bashoLabelJp(basho),
							inProgress,
							cell(aId, aByBasho.get(id), aMatchesByBasho.getOrDefault(id, List.of()), inProgress),
							cell(bId, bByBasho.get(id), bMatchesByBasho.getOrDefault(id, List.of()), inProgress));
				})
				.toList();
	}

	private static Map<Integer, BanzukeEntity> byBasho(List<BanzukeEntity> banzuke) {
		return banzuke.stream().collect(Collectors.toMap(z -> z.getBashoEntity().getId(), Function.identity(), (x, y) -> x));
	}

	/**
	 * 한 바쇼 성적 칸. 공식 성적(반즈케 임포트)이 경기 기록보다 뒤처지면(대회 중) 경기 기록으로 센다 —
	 * 호시토리표(RikishiDetailService#toBashoGameLog)와 같은 규칙.
	 */
	private CompareDTO.Cell cell(Integer rikishiId, BanzukeEntity banzuke, List<TorikumiEntity> matches, boolean inProgress) {
		if (banzuke == null) {
			return null;
		}
		long decided = matches.stream().filter(t -> t.getWinnerRikishiEntity() != null).count();
		boolean useRecord = banzuke.hasRecord() && banzuke.getWins() + banzuke.getLosses() >= decided;
		long wins;
		long losses;
		long absences;
		if (useRecord) {
			wins = banzuke.getWins();
			losses = banzuke.getLosses();
			absences = banzuke.getAbsences();
		} else {
			wins = matches.stream().filter(t -> isWinner(t, rikishiId)).count();
			losses = decided - wins;
			absences = 0;
		}
		boolean hasResult = wins + losses + absences > 0;
		String recordKr = hasResult ? wins + "승 " + losses + "패" + (absences > 0 ? " " + absences + "휴" : "") : "-";
		String recordJp = hasResult ? wins + "勝" + losses + "敗" + (absences > 0 ? absences + "休" : "") : "-";
		// 휴장은 패와 같이 센다 (휴장이 많으면 마케코시). 진행 중인 바쇼는 아직 정해지지 않았으니 표시 안 함
		boolean decidedRecord = hasResult && !inProgress;
		return new CompareDTO.Cell(
				withSide(RankDisplayUtil.sideDisplayKorean(banzuke.getSide()),
						RankDisplayUtil.rankDisplayKorean(banzuke.getRankName(), banzuke.getRankValue())),
				withSide(RankDisplayUtil.sideDisplay(banzuke.getSide()),
						RankDisplayUtil.rankDisplay(banzuke.getRankName(), banzuke.getRankValue())),
				recordKr,
				recordJp,
				decidedRecord && wins > losses + absences,
				decidedRecord && wins < losses + absences);
	}

	/** 기술로 이긴 경기만 (부전·결정기술 미기록 제외) - 결정기술 집계용 */
	private static List<TorikumiEntity> wonByTechnique(List<TorikumiEntity> h2h, Integer winnerId) {
		return h2h.stream()
				.filter(t -> isWinner(t, winnerId))
				.filter(t -> t.getResultType() != ResultType.FUZEN && t.getKimarite() != null && !t.getKimarite().isBlank())
				.toList();
	}

	/** 결정기술별 횟수, 많은 순 (같으면 먼저 나온 = 최근 경기의 기술이 앞) */
	private static List<CompareDTO.KimariteCount> kimariteCounts(List<TorikumiEntity> won) {
		Map<String, long[]> counts = new LinkedHashMap<>();
		Map<String, String> jpByKr = new LinkedHashMap<>();
		for (TorikumiEntity t : won) {
			String kr = KimariteDisplayUtil.toKr(t.getKimarite());
			counts.computeIfAbsent(kr, k -> new long[1])[0]++;
			jpByKr.putIfAbsent(kr, KimariteDisplayUtil.toJp(t.getKimarite()));
		}
		return counts.entrySet().stream()
				.sorted(Comparator.comparingLong((Map.Entry<String, long[]> e) -> e.getValue()[0]).reversed())
				.map(e -> new CompareDTO.KimariteCount(e.getKey(), jpByKr.get(e.getKey()), e.getValue()[0]))
				.toList();
	}

	/** 키마리테 분류별 횟수·비율. 협회 분류 순서대로, 0회인 분류는 뺀다 */
	private static List<CompareDTO.CategoryCount> categoryCounts(List<TorikumiEntity> won) {
		if (won.isEmpty()) {
			return List.of();
		}
		Map<KimariteCategory, Long> counts = new EnumMap<>(KimariteCategory.class);
		for (TorikumiEntity t : won) {
			counts.merge(KimariteDisplayUtil.categoryOf(t.getKimarite()), 1L, Long::sum);
		}
		return counts.entrySet().stream()
				.map(e -> new CompareDTO.CategoryCount(e.getKey().getNameKr(), e.getKey().getNameJp(), e.getValue(),
						Math.round(e.getValue() * 1000.0 / won.size()) / 10.0))
				.toList();
	}

	private CompareDTO.Bout toBout(TorikumiEntity t, Integer aId) {
		boolean aWon = isWinner(t, aId);
		boolean fusen = t.getResultType() == ResultType.FUZEN;
		String kimariteKr;
		String kimariteJp;
		if (fusen) {
			kimariteKr = "부전";
			kimariteJp = "不戦";
		} else if (t.getKimarite() != null && !t.getKimarite().isBlank()) {
			kimariteKr = KimariteDisplayUtil.toKr(t.getKimarite());
			kimariteJp = KimariteDisplayUtil.toJp(t.getKimarite());
		} else {
			kimariteKr = "-";
			kimariteJp = "-";
		}
		BashoEntity basho = t.getBashoEntity();
		return new CompareDTO.Bout(t.getId(), bashoLabelKr(basho), bashoLabelJp(basho), t.getDay(),
				aWon, fusen, kimariteKr, kimariteJp);
	}

	private CompareDTO.Side toSide(RikishiEntity r, BanzukeEntity latest) {
		String rankKr = null;
		String rankJp = null;
		if (latest != null) {
			rankKr = withSide(RankDisplayUtil.sideDisplayKorean(latest.getSide()),
					RankDisplayUtil.rankDisplayKorean(latest.getRankName(), latest.getRankValue()));
			rankJp = withSide(RankDisplayUtil.sideDisplay(latest.getSide()),
					RankDisplayUtil.rankDisplay(latest.getRankName(), latest.getRankValue()));
		}
		return new CompareDTO.Side(
				r.getId(),
				displayNameKr(r),
				displayNameJp(r),
				r.getHeyaEntity() != null ? r.getHeyaEntity().getNameKr() : null,
				r.getHeyaEntity() != null ? r.getHeyaEntity().getNameJp() : null,
				rankKr,
				rankJp,
				r.isActive());
	}

	private RikishiEntity find(Integer rikishiId) {
		return rikishiRepository.findById(rikishiId)
				.orElseThrow(() -> new EntityNotFoundException("리키시를 찾을 수 없습니다. id=" + rikishiId));
	}

	private static boolean involves(TorikumiEntity t, Integer rikishiId) {
		return t.getEastRikishiEntity().getId().equals(rikishiId) || t.getWestRikishiEntity().getId().equals(rikishiId);
	}

	private static boolean isWinner(TorikumiEntity t, Integer rikishiId) {
		return t.getWinnerRikishiEntity() != null && t.getWinnerRikishiEntity().getId().equals(rikishiId);
	}

	/** "2026 아키바쇼" - 표 한 줄에 들어가게 월은 뺀다 */
	private static String bashoLabelKr(BashoEntity basho) {
		return basho.getBashoYear() + " " + basho.getBashoMonth().getDisplayNameKr();
	}

	/** "2026年秋場所" */
	private static String bashoLabelJp(BashoEntity basho) {
		return basho.getBashoYear() + "年" + basho.getBashoMonth().getDisplayNameJp();
	}

	private static String withSide(String side, String rank) {
		if (side == null) {
			return rank;
		}
		return rank == null ? side : side + " " + rank;
	}

	// 한국어 시코나가 아직 없으면(임포트 직후) 한자 → 로마자. 성명이 붙어 있으면 시코나만 (RikishiDetailService와 같은 규칙)
	private static String displayNameKr(RikishiEntity r) {
		String kr = firstToken(r.getShikonaKr());
		if (kr != null && !kr.isBlank()) {
			return kr;
		}
		return r.getShikonaJp() != null && !r.getShikonaJp().isBlank() ? r.getShikonaJp() : firstToken(r.getShikonaEn());
	}

	private static String displayNameJp(RikishiEntity r) {
		String jp = firstToken(r.getShikonaJp());
		return jp != null && !jp.isBlank() ? jp : firstToken(r.getShikonaEn());
	}

	private static String firstToken(String value) {
		if (value == null) {
			return null;
		}
		int idx = value.indexOf(' ');
		return idx > 0 ? value.substring(0, idx) : value;
	}
}
