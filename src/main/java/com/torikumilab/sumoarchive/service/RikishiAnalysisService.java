package com.torikumilab.sumoarchive.service;

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
	static final int H2H_TOP = 3;

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
	 */
	public HeadToHeadHighlightsDTO getHeadToHeadHighlights(Integer rikishiId, List<HeadToHeadDTO> headToHead) {
		// 부전은 실제로 붙지 않은 경기라 상성과 무관 - 빼고 센다.
		record Candidate(HeadToHeadDTO h, long wins, long bouts) {
		}
		List<Candidate> candidates = new ArrayList<>();
		for (HeadToHeadDTO h : headToHead) {
			long bouts = h.bouts().stream().filter(b -> !b.fusen()).count();
			long wins = h.bouts().stream().filter(b -> !b.fusen() && b.win()).count();
			if (bouts >= H2H_MIN_BOUTS) {
				candidates.add(new Candidate(h, wins, bouts));
			}
		}
		if (candidates.isEmpty()) {
			return new HeadToHeadHighlightsDTO(H2H_MIN_BOUTS, H2H_MIN_DIFF, List.of(), List.of());
		}

		List<Integer> ids = new ArrayList<>();
		ids.add(rikishiId);
		candidates.forEach(c -> ids.add(c.h().opponentId()));
		Map<Integer, Double> winRate = new HashMap<>();
		for (WinRateRow r : torikumiRepository.findWinRates(ids)) {
			winRate.put(r.getRikishiId(), (r.getWins().doubleValue() + 1) / (r.getBouts().doubleValue() + 2));
		}
		double myRate = winRate.getOrDefault(rikishiId, 0.5);

		List<HeadToHeadHighlightsDTO.Row> rows = new ArrayList<>();
		for (Candidate c : candidates) {
			double oppRate = winRate.getOrDefault(c.h().opponentId(), 0.5);
			double expected = myRate * (1 - oppRate) / (myRate * (1 - oppRate) + oppRate * (1 - myRate));
			double diff = c.wins() - c.bouts() * expected;
			if (Math.abs(diff) >= H2H_MIN_DIFF) {
				rows.add(new HeadToHeadHighlightsDTO.Row(c.h().opponentId(), c.h().opponentShikonaKr(),
						c.h().opponentShikonaJp(), c.wins(), c.bouts() - c.wins(), round1(diff)));
			}
		}

		List<HeadToHeadHighlightsDTO.Row> tough = rows.stream()
				.filter(r -> r.diff() < 0)
				.sorted(Comparator.comparingDouble(HeadToHeadHighlightsDTO.Row::diff))
				.limit(H2H_TOP)
				.toList();
		List<HeadToHeadHighlightsDTO.Row> favorable = rows.stream()
				.filter(r -> r.diff() > 0)
				.sorted(Comparator.comparingDouble(HeadToHeadHighlightsDTO.Row::diff).reversed())
				.limit(H2H_TOP)
				.toList();
		return new HeadToHeadHighlightsDTO(H2H_MIN_BOUTS, H2H_MIN_DIFF, tough, favorable);
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
