package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.domain.dto.BashoGameLogDTO;
import com.torikumilab.sumoarchive.domain.dto.CareerSummaryDTO;
import com.torikumilab.sumoarchive.domain.dto.HeadToHeadDTO;
import com.torikumilab.sumoarchive.domain.dto.HeadToHeadHighlightsDTO;
import com.torikumilab.sumoarchive.domain.dto.LossTypeSummaryDTO;
import com.torikumilab.sumoarchive.domain.dto.OpponentConditionDTO;
import com.torikumilab.sumoarchive.domain.entity.constant.Division;
import com.torikumilab.sumoarchive.domain.entity.constant.LossType;
import com.torikumilab.sumoarchive.repository.BashoRepository;
import com.torikumilab.sumoarchive.repository.TorikumiRepository;
import com.torikumilab.sumoarchive.repository.TorikumiRepository.DivisionKimariteCountRow;
import com.torikumilab.sumoarchive.repository.TorikumiRepository.OpponentBoutRow;
import com.torikumilab.sumoarchive.repository.TorikumiRepository.PhysiqueDiffRow;
import com.torikumilab.sumoarchive.repository.TorikumiRepository.WinRateRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 리키시 프로필 분석 - "패배 유형"(어떻게 졌나 + 리그 평균 대비)과 "상대 조건별 성적"(계급 / 체중 차 / 키 차).
 * 리그 평균은 캐시 없이 집계 쿼리로 매번 계산한다 (임포트·관리자 수정 뒤에도 항상 최신이고, 지금 규모에선 가벼움).
 */
@Service
@RequiredArgsConstructor
public class RikishiAnalysisService {

	// ===== 패배 유형 약점/강점 판정 기준 =====
	/** 총 패배가 이보다 적으면 표본 부족 - 판정하지 않는다. */
	static final int MIN_LOSSES = 20;
	/**
	 * 약점: 그 유형 패배가 최소 이만큼은 있어야 한다.
	 * 강점: 평균대로라면 이만큼은 졌을 유형이어야 한다 (원래 드문 유형에서 0건인 걸 강점으로 보지 않게).
	 */
	static final int MIN_TYPE_LOSSES = 5;
	/** 평균 대비 배율 (약점: 이상, 강점: 이하) */
	static final double WEAK_RATIO = 1.4;
	static final double STRONG_RATIO = 0.6;
	/** 평균과의 최소 차이 (%p) - 원래 비율이 작은 유형에서 배율만 크게 튀는 걸 막는다. */
	static final double MIN_GAP = 8.0;

	// ===== 상대 조건별 성적 =====
	/** 구간 경기 수가 이보다 적으면 흐리게 "표본 적음" */
	static final int MIN_BOUTS = 10;

	// ===== 상대전적 "유독 약한/강한 상대" =====
	/** 부전 제외 맞대결이 이보다 적은 상대는 후보에서 뺀다. */
	static final int H2H_MIN_BOUTS = 3;
	/** 실제 승수와 예상 승수의 차이가 이 이상이어야 "유독" 약하거나 강하다고 본다. */
	static final double H2H_MIN_DIFF = 1.5;
	/**
	 * 또는: 이만큼 이상 맞붙었고 실제 승률이 예상 승률과 이만큼(비율) 이상 차이 나면.
	 * 강자 상대는 예상 승수 자체가 작아 "승수 차이"로는 잘 안 잡혀서 (예: 예상 39% → 실제 1승 5패 17%, 차이 1.3승)
	 */
	static final int H2H_RATE_MIN_BOUTS = 5;
	static final double H2H_MIN_RATE_GAP = 0.20;
	/**
	 * 그리고 실제 전적도 확실히 한쪽으로 기울어야 한다 - 예상 대비만 보면 7승 8패처럼 비등한 전적도
	 * 상대가 약하면 "천적"이 돼 버려서. 약한 상대는 승률 40% 이하, 강한 상대는 60% 이상.
	 */
	static final double H2H_TOUGH_MAX_RATE = 0.40;
	static final double H2H_FAVORABLE_MIN_RATE = 0.60;
	/** 그리고 승패 차이가 이만큼 이상 (2승 3패처럼 1승 차이는 상성이라 보기 어려움) */
	static final int H2H_MIN_MARGIN = 2;
	/** 현역 선수 페이지는 최근 이 기간 안에 붙은 상대만 (몇 년 전 몇 번 붙은 상대는 지금의 상성과 무관) */
	static final int H2H_RECENT_YEARS = 3;
	static final int H2H_TOP = 3;

	// ===== 커리어 요약 =====
	// 카치코시는 "치를 경기의 과반 승리"라 디비전마다 다르다 (세키토리 15번 → 8승, 마쿠시타 이하 7번 → 4승).
	static final int DOUBLE_DIGIT_WINS = 10;

	private final TorikumiRepository torikumiRepository;
	private final BashoRepository bashoRepository;

	/**
	 * 패배 유형 요약. 기간 필터는 키마리테 차트와 같은 규칙(두 바쇼 시작일 사이, 없으면 통산).
	 * 리그 평균은 기간과 무관하게 전체 데이터 기준 - 선수 쪽 기간을 좁혀도 비교 기준이 흔들리지 않게.
	 */
	public LossTypeSummaryDTO getLossTypes(Integer rikishiId, Integer fromBashoId, Integer toBashoId) {
		LocalDate[] range = bashoRepository.findStartDateRange(fromBashoId, toBashoId);
		List<DivisionKimariteCountRow> myLosses = range == null
				? torikumiRepository.findLossKimariteByDivision(rikishiId)
				: torikumiRepository.findLossKimariteByDivisionBetween(rikishiId, range[0], range[1]);

		Map<LossType, Long> countByType = new EnumMap<>(LossType.class);
		Map<Division, Long> lossesByDivision = new EnumMap<>(Division.class);
		for (DivisionKimariteCountRow row : myLosses) {
			countByType.merge(LossType.of(row.getKimarite()), row.getCnt(), Long::sum);
			lossesByDivision.merge(row.getDivision(), row.getCnt(), Long::sum);
		}
		long total = lossesByDivision.values().stream().mapToLong(Long::longValue).sum();
		if (total == 0) {
			return new LossTypeSummaryDTO(0, true, 0, List.of());
		}

		// 디비전마다 결정기술 분포가 달라서(쥬료는 押し 비율이 높고 投げ가 낮음) 마쿠우치·쥬료를 오간 선수도
		// 공정하게 비교하도록, 디비전별 리그 비율을 이 선수가 그 디비전에서 진 경기 수로 가중 평균한다.
		Map<Division, Map<LossType, Long>> leagueByDivision = new EnumMap<>(Division.class);
		Map<Division, Long> leagueTotalByDivision = new EnumMap<>(Division.class);
		for (DivisionKimariteCountRow row : torikumiRepository.findKimariteByDivision()) {
			leagueByDivision.computeIfAbsent(row.getDivision(), d -> new EnumMap<>(LossType.class))
					.merge(LossType.of(row.getKimarite()), row.getCnt(), Long::sum);
			leagueTotalByDivision.merge(row.getDivision(), row.getCnt(), Long::sum);
		}

		boolean smallSample = total < MIN_LOSSES;
		List<LossTypeSummaryDTO.Row> rows = new ArrayList<>();
		double scaleMax = 0;
		for (LossType type : LossType.values()) {
			long count = countByType.getOrDefault(type, 0L);
			double percent = count * 100.0 / total;

			double expected = 0;
			for (Map.Entry<Division, Long> e : lossesByDivision.entrySet()) {
				long leagueTotal = leagueTotalByDivision.getOrDefault(e.getKey(), 0L);
				if (leagueTotal == 0) {
					continue;
				}
				long leagueCount = leagueByDivision.getOrDefault(e.getKey(), Map.of()).getOrDefault(type, 0L);
				expected += e.getValue() * ((double) leagueCount / leagueTotal);
			}
			expected = expected * 100.0 / total;

			LossTypeSummaryDTO.Verdict verdict = (smallSample || type == LossType.ETC)
					? LossTypeSummaryDTO.Verdict.NONE
					: judge(count, percent, expected, total);

			rows.add(new LossTypeSummaryDTO.Row(type.getNameKr(), type.getNameJp(), count,
					round1(percent), round1(expected), verdict));
			scaleMax = Math.max(scaleMax, Math.max(percent, expected));
		}
		return new LossTypeSummaryDTO(total, smallSample, round1(scaleMax), rows);
	}

	private LossTypeSummaryDTO.Verdict judge(long count, double percent, double expected, long total) {
		if (count >= MIN_TYPE_LOSSES && percent >= expected * WEAK_RATIO && percent - expected >= MIN_GAP) {
			return LossTypeSummaryDTO.Verdict.WEAK;
		}
		double expectedCount = expected * total / 100.0;
		if (expectedCount >= MIN_TYPE_LOSSES && percent <= expected * STRONG_RATIO && expected - percent >= MIN_GAP) {
			return LossTypeSummaryDTO.Verdict.STRONG;
		}
		return LossTypeSummaryDTO.Verdict.NONE;
	}

	/**
	 * 상대 조건별 성적 (통산). 계급은 그 바쇼 상대 반즈케 기준, 체격 차는 양쪽 현재 키·체중 기준.
	 * 체격 구간에는 같은 구간의 리그 전체 승률을 같이 내려 "이 선수만의 경향인지" 비교할 수 있게 한다.
	 */
	public OpponentConditionDTO getOpponentConditions(Integer rikishiId) {
		Map<RankBucket, Tally> byRank = new EnumMap<>(RankBucket.class);
		Map<WeightBucket, Tally> byWeight = new EnumMap<>(WeightBucket.class);
		Map<HeightBucket, Tally> byHeight = new EnumMap<>(HeightBucket.class);

		for (OpponentBoutRow b : torikumiRepository.findOpponentBouts(rikishiId)) {
			boolean win = b.getWin() != null && b.getWin().intValue() == 1;
			RankBucket rank = RankBucket.of(b.getOppRankName(), b.getOppRankValue());
			if (rank != null) {
				byRank.computeIfAbsent(rank, k -> new Tally()).add(1, win ? 1 : 0);
			}
			if (b.getWeightDiff() != null) {
				byWeight.computeIfAbsent(WeightBucket.of(b.getWeightDiff().doubleValue()), k -> new Tally()).add(1, win ? 1 : 0);
			}
			if (b.getHeightDiff() != null) {
				byHeight.computeIfAbsent(HeightBucket.of(b.getHeightDiff().doubleValue()), k -> new Tally()).add(1, win ? 1 : 0);
			}
		}

		// 리그 평균: 한 경기를 동쪽·서쪽 두 시점으로 센다 (서쪽 시점은 차이 부호가 반대, 승수는 경기 수 - 동쪽 승수).
		Map<WeightBucket, Tally> leagueWeight = new EnumMap<>(WeightBucket.class);
		Map<HeightBucket, Tally> leagueHeight = new EnumMap<>(HeightBucket.class);
		for (PhysiqueDiffRow r : torikumiRepository.findPhysiqueDiffStats()) {
			long bouts = r.getBouts().longValue();
			long eastWins = r.getEastWins().longValue();
			if (r.getWeightDiff() != null) {
				double dw = r.getWeightDiff().doubleValue();
				leagueWeight.computeIfAbsent(WeightBucket.of(dw), k -> new Tally()).add(bouts, eastWins);
				leagueWeight.computeIfAbsent(WeightBucket.of(-dw), k -> new Tally()).add(bouts, bouts - eastWins);
			}
			if (r.getHeightDiff() != null) {
				double dh = r.getHeightDiff().doubleValue();
				leagueHeight.computeIfAbsent(HeightBucket.of(dh), k -> new Tally()).add(bouts, eastWins);
				leagueHeight.computeIfAbsent(HeightBucket.of(-dh), k -> new Tally()).add(bouts, bouts - eastWins);
			}
		}

		// 계급은 만난 적 있는 구간만 (쥬료 선수가 요코즈나 줄을 매번 0전으로 보는 건 의미가 없어서),
		// 체격은 구간이 고정 4·3개라 경기가 없어도 "0전"으로 전부 보여준다.
		List<OpponentConditionDTO.Row> rankRows = new ArrayList<>();
		for (RankBucket bucket : RankBucket.values()) {
			Tally t = byRank.get(bucket);
			if (t != null) {
				rankRows.add(toRow(bucket.labelKr, bucket.labelJp, t, null));
			}
		}
		return new OpponentConditionDTO(
				rankRows,
				physiqueRows(WeightBucket.values(), byWeight, leagueWeight, w -> w.labelKr, w -> w.labelJp),
				physiqueRows(HeightBucket.values(), byHeight, leagueHeight, h -> h.labelKr, h -> h.labelJp));
	}

	/**
	 * 상대전적 요약 - 두 선수의 평소 승률로 예상한 것보다 유독 크게 지거나 이긴 상대.
	 * 예상 승률은 log5 방식: pA(1-pB) / (pA(1-pB) + pB(1-pA)). 상대가 강하면 예상이 저절로 낮아져서
	 * 반즈케를 따로 따지지 않아도 "요코즈나에게 진 것"이 천적으로 잡히지 않는다.
	 * 평소 승률은 (승+1)/(경기+2)로 살짝 보정 - 전승·전패 선수가 있어도 식이 0으로 나뉘지 않게.
	 *
	 * @param headToHead RikishiDetailService.getHeadToHead 결과 (재조회 없이 그대로 사용)
	 * @param currentOnly 현역 선수 페이지 - 현역 상대이면서 최근 3년 안에 붙은 상대만 후보로
	 *                    (은퇴한 옛 상대나 오래전에 붙은 상대가 지금의 천적으로 보이지 않게). 은퇴 선수는 false(커리어 전체)
	 */
	public HeadToHeadHighlightsDTO getHeadToHeadHighlights(Integer rikishiId, List<HeadToHeadDTO> headToHead,
														   boolean currentOnly) {
		LocalDate recentCutoff = LocalDate.now().minusYears(H2H_RECENT_YEARS);
		// 부전은 실제로 붙지 않은 경기라 상성과 무관 - 빼고 센다.
		// sekitori = 맞대결 대부분이 세키토리 무대였는지 → 평소 승률도 같은 무대의 경기로 잰다.
		record Candidate(HeadToHeadDTO h, long wins, long bouts, boolean sekitori) {
		}
		List<Candidate> candidates = new ArrayList<>();
		for (HeadToHeadDTO h : headToHead) {
			if (currentOnly && (!h.opponentActive() || !metSince(h, recentCutoff))) {
				continue;
			}
			long bouts = h.bouts().stream().filter(b -> !b.fusen()).count();
			long wins = h.bouts().stream().filter(b -> !b.fusen() && b.win()).count();
			long sekitoriBouts = h.bouts().stream().filter(b -> !b.fusen() && b.sekitori()).count();
			if (bouts >= H2H_MIN_BOUTS) {
				candidates.add(new Candidate(h, wins, bouts, sekitoriBouts * 2 >= bouts));
			}
		}
		if (candidates.isEmpty()) {
			return new HeadToHeadHighlightsDTO(H2H_MIN_BOUTS, H2H_MIN_DIFF, List.of(), List.of());
		}

		List<Integer> ids = new ArrayList<>();
		ids.add(rikishiId);
		candidates.forEach(c -> ids.add(c.h().opponentId()));
		// [0] = 세키토리 경기, [1] = 전체 경기의 {경기 수, 승수}
		Map<Integer, long[][]> tally = new HashMap<>();
		for (WinRateRow r : torikumiRepository.findWinRates(ids)) {
			long[][] t = tally.computeIfAbsent(r.getRikishiId(), k -> new long[2][2]);
			if (r.getSekitori() != null && r.getSekitori().intValue() == 1) {
				t[0][0] += r.getBouts().longValue();
				t[0][1] += r.getWins().longValue();
			}
			t[1][0] += r.getBouts().longValue();
			t[1][1] += r.getWins().longValue();
		}

		List<HeadToHeadHighlightsDTO.Row> rows = new ArrayList<>();
		for (Candidate c : candidates) {
			double myRate = smoothedRate(tally.get(rikishiId), c.sekitori());
			double oppRate = smoothedRate(tally.get(c.h().opponentId()), c.sekitori());
			double expected = myRate * (1 - oppRate) / (myRate * (1 - oppRate) + oppRate * (1 - myRate));
			double diff = c.wins() - c.bouts() * expected;
			double actualRate = (double) c.wins() / c.bouts();
			double rateGap = actualRate - expected;
			boolean byRate = c.bouts() >= H2H_RATE_MIN_BOUTS && Math.abs(rateGap) >= H2H_MIN_RATE_GAP;
			long margin = Math.abs(c.wins() - (c.bouts() - c.wins()));
			boolean lopsided = margin >= H2H_MIN_MARGIN
					&& (diff < 0 ? actualRate <= H2H_TOUGH_MAX_RATE : actualRate >= H2H_FAVORABLE_MIN_RATE);
			if ((Math.abs(diff) >= H2H_MIN_DIFF || byRate) && lopsided) {
				rows.add(new HeadToHeadHighlightsDTO.Row(c.h().opponentId(), c.h().opponentShikonaKr(),
						c.h().opponentShikonaJp(), c.wins(), c.bouts() - c.wins(), round1(diff)));
			}
		}

		// 자격은 "예상 대비"로 거르고, 순서는 실제 전적이 확실히 기운 순 (천적 = 실제로 훨씬 많이 지는 상대).
		// 예상 대비 순이면 원래 이길 상대에게 진 1승 4패가 강자에게 진 1승 5패보다 위로 올라가고,
		// 그냥 승률 순이면 0승 3패가 3승 16패보다 위로 올라가서 → 표본 크기를 감안한 Wilson 구간으로.
		// 천적은 승률 상한이 낮은 순(높게 봐도 이 정도), 강한 상대는 하한이 높은 순(낮게 봐도 이 정도).
		List<HeadToHeadHighlightsDTO.Row> tough = rows.stream()
				.filter(r -> r.diff() < 0)
				.sorted(Comparator.comparingDouble((HeadToHeadHighlightsDTO.Row r) -> wilson(r, true))
						.thenComparingDouble(HeadToHeadHighlightsDTO.Row::diff))
				.limit(H2H_TOP)
				.toList();
		List<HeadToHeadHighlightsDTO.Row> favorable = rows.stream()
				.filter(r -> r.diff() > 0)
				.sorted(Comparator.comparingDouble((HeadToHeadHighlightsDTO.Row r) -> wilson(r, false)).reversed()
						.thenComparing(Comparator.comparingDouble(HeadToHeadHighlightsDTO.Row::diff).reversed()))
				.limit(H2H_TOP)
				.toList();
		return new HeadToHeadHighlightsDTO(H2H_MIN_BOUTS, H2H_MIN_DIFF, tough, favorable);
	}

	/**
	 * 평소 승률 (승+1)/(경기+2). sekitori면 세키토리 경기만, 아니면 전체 경기로.
	 * 세키토리 경기가 아예 없으면(드묾) 전체로 대체, 기록이 없으면 0.5.
	 */
	/** 이 상대와 cutoff 이후 시작한 바쇼에서 붙은 적이 있는지 (바쇼 시작일이 없는 수동 데이터는 최근으로 본다) */
	private static boolean metSince(HeadToHeadDTO h, LocalDate cutoff) {
		return h.bouts().stream()
				.anyMatch(b -> b.bashoStart() == null || !b.bashoStart().isBefore(cutoff));
	}

	/**
	 * 맞대결 승률의 Wilson 95% 구간 상한(upper=true) 또는 하한. 경기 수가 적을수록 구간이 넓어서
	 * 0승 3패의 상한(약 56%)이 3승 16패의 상한(약 38%)보다 높게 나온다.
	 */
	private static double wilson(HeadToHeadHighlightsDTO.Row r, boolean upper) {
		double n = r.wins() + r.losses();
		if (n == 0) {
			return upper ? 1 : 0;
		}
		double z = 1.96;
		double p = r.wins() / n;
		double denom = 1 + z * z / n;
		double center = (p + z * z / (2 * n)) / denom;
		double margin = z * Math.sqrt(p * (1 - p) / n + z * z / (4 * n * n)) / denom;
		return upper ? center + margin : center - margin;
	}

	private static double smoothedRate(long[][] t, boolean sekitori) {
		if (t == null) {
			return 0.5;
		}
		long[] pick = (sekitori && t[0][0] > 0) ? t[0] : t[1];
		return (pick[1] + 1.0) / (pick[0] + 2.0);
	}

	/**
	 * "바쇼별 성적" 패널 맨 위 커리어 요약. 끝난 바쇼를 전 디비전에서 센다 (CareerSummaryDTO 참고).
	 * 카치코시 = 승 + 패 + 휴(치를 경기 수)의 과반 승리 → 세키토리 8승, 마쿠시타 이하 4승.
	 * 휴장이 낀 바쇼도 과반이 안 되면 마케코시 (예: 7승 5패 3휴), 全休도 분모에 포함 - 반즈케에서도 마케코시 취급.
	 * 두 자릿수 승리·최고 성적은 15번 싸우는 세키토리 바쇼만 (하위 리그는 최대 7승이라 같은 잣대가 아니다).
	 *
	 * @param gameLog RikishiDetailService.getGameLog 결과 (최신 바쇼부터, 재조회 없이 그대로 사용)
	 */
	public CareerSummaryDTO getCareerSummary(List<BashoGameLogDTO> gameLog) {
		int basho = 0, kachikoshi = 0, doubleDigit = 0, absence = 0, zenkyu = 0;
		BashoGameLogDTO best = null;
		for (BashoGameLogDTO g : gameLog) {
			if (!g.completed()) {
				continue;
			}
			basho++;
			int scheduled = g.wins() + g.losses() + g.absences();
			if (g.wins() * 2 > scheduled) {
				kachikoshi++;
			}
			if (g.absences() > 0) {
				absence++;
			}
			boolean sekitori = g.division() == Division.Makuuchi || g.division() == Division.Juryo;
			if (g.wins() == 0 && g.losses() == 0) {
				zenkyu++;
			} else if (sekitori) {
				if (g.wins() >= DOUBLE_DIGIT_WINS) {
					doubleDigit++;
				}
				if (best == null || g.wins() > best.wins()) {
					// 최신 바쇼부터 돌기 때문에 승수가 같으면 먼저 본(최근) 바쇼가 남는다.
					best = g;
				}
			}
		}
		return new CareerSummaryDTO(basho, kachikoshi, doubleDigit, absence, zenkyu, best);
	}

	private <B extends Enum<B>> List<OpponentConditionDTO.Row> physiqueRows(B[] buckets, Map<B, Tally> mine,
																		   Map<B, Tally> league,
																		   Function<B, String> labelKr,
																		   Function<B, String> labelJp) {
		List<OpponentConditionDTO.Row> rows = new ArrayList<>();
		for (B bucket : buckets) {
			rows.add(toRow(labelKr.apply(bucket), labelJp.apply(bucket),
					mine.getOrDefault(bucket, new Tally()), league.get(bucket)));
		}
		return rows;
	}

	private OpponentConditionDTO.Row toRow(String labelKr, String labelJp, Tally mine, Tally league) {
		Double leaguePct = (league != null && league.bouts > 0) ? round1(league.wins * 100.0 / league.bouts) : null;
		Double winPct = mine.bouts > 0 ? round1(mine.wins * 100.0 / mine.bouts) : null;
		return new OpponentConditionDTO.Row(labelKr, labelJp, mine.bouts, mine.wins, winPct, leaguePct,
				mine.bouts < MIN_BOUTS);
	}

	private static double round1(double value) {
		return Math.round(value * 10) / 10.0;
	}

	private static final class Tally {
		long bouts;
		long wins;

		void add(long bouts, long wins) {
			this.bouts += bouts;
			this.wins += wins;
		}
	}

	/** 상대의 그 바쇼 계급 구간. 마에가시라는 상위(1~8)/하위(9~)로 나눈다. */
	enum RankBucket {
		YOKOZUNA_OZEKI("요코즈나·오제키", "横綱・大関"),
		SEKIWAKE_KOMUSUBI("세키와케·코무스비", "関脇・小結"),
		MAEGASHIRA_UPPER("마에가시라 1~8", "前頭1〜8枚目"),
		MAEGASHIRA_LOWER("마에가시라 9 이하", "前頭9枚目以下"),
		JURYO("주료", "十両"),
		LOWER("마쿠시타 이하", "幕下以下");

		final String labelKr;
		final String labelJp;

		RankBucket(String labelKr, String labelJp) {
			this.labelKr = labelKr;
			this.labelJp = labelJp;
		}

		/** banzuke.rank_name 저장값(RankName enum 이름) 기준. 반즈케가 없으면 null. */
		static RankBucket of(String rankName, Integer rankValue) {
			if (rankName == null) {
				return null;
			}
			return switch (rankName) {
				case "Yokozuna", "Ozeki" -> YOKOZUNA_OZEKI;
				case "Sekiwake", "Komusubi" -> SEKIWAKE_KOMUSUBI;
				case "Maegashira" -> (rankValue != null && rankValue <= 8) ? MAEGASHIRA_UPPER : MAEGASHIRA_LOWER;
				case "Juryo" -> JURYO;
				default -> LOWER;
			};
		}
	}

	/** 체중 차 (상대 - 본인, kg). 차이 0은 "0~20kg 무거운 상대" 쪽. */
	enum WeightBucket {
		MUCH_LIGHTER("20kg 이상 가벼운 상대", "20kg以上軽い相手"),
		LIGHTER("0~20kg 가벼운 상대", "0〜20kg軽い相手"),
		HEAVIER("0~20kg 무거운 상대", "0〜20kg重い相手"),
		MUCH_HEAVIER("20kg 이상 무거운 상대", "20kg以上重い相手");

		final String labelKr;
		final String labelJp;

		WeightBucket(String labelKr, String labelJp) {
			this.labelKr = labelKr;
			this.labelJp = labelJp;
		}

		static WeightBucket of(double diff) {
			if (diff <= -20) {
				return MUCH_LIGHTER;
			}
			if (diff < 0) {
				return LIGHTER;
			}
			return diff < 20 ? HEAVIER : MUCH_HEAVIER;
		}
	}

	/** 키 차 (상대 - 본인, cm). */
	enum HeightBucket {
		SHORTER("5cm 이상 작은 상대", "5cm以上低い相手"),
		SIMILAR("키 비슷한 상대 (±5cm 미만)", "身長が近い相手(±5cm未満)"),
		TALLER("5cm 이상 큰 상대", "5cm以上高い相手");

		final String labelKr;
		final String labelJp;

		HeightBucket(String labelKr, String labelJp) {
			this.labelKr = labelKr;
			this.labelJp = labelJp;
		}

		static HeightBucket of(double diff) {
			if (diff <= -5) {
				return SHORTER;
			}
			return diff < 5 ? SIMILAR : TALLER;
		}
	}
}
