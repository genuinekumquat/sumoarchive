package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.domain.dto.LossTypeSummaryDTO;
import com.torikumilab.sumoarchive.domain.dto.LossTypeSummaryDTO.Verdict;
import com.torikumilab.sumoarchive.domain.dto.OpponentConditionDTO;
import com.torikumilab.sumoarchive.domain.entity.constant.Division;
import com.torikumilab.sumoarchive.domain.entity.constant.LossType;
import com.torikumilab.sumoarchive.repository.BashoRepository;
import com.torikumilab.sumoarchive.repository.TorikumiRepository;
import com.torikumilab.sumoarchive.repository.TorikumiRepository.DivisionKimariteCountRow;
import com.torikumilab.sumoarchive.repository.TorikumiRepository.OpponentBoutRow;
import com.torikumilab.sumoarchive.repository.TorikumiRepository.PhysiqueDiffRow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RikishiAnalysisServiceTest {

	private static final int ID = 1;

	@Mock
	private TorikumiRepository torikumiRepository;
	@Mock
	private BashoRepository bashoRepository;

	@InjectMocks
	private RikishiAnalysisService service;

	// 마쿠우치 리그 분포: 밀려서 30 / 잡혀서 30 / 고꾸라짐 20 / 던져짐 15 / 뒤 3 / 기타 2 (합 100)
	private static final List<DivisionKimariteCountRow> MAKUUCHI_LEAGUE = List.of(
			row(Division.Makuuchi, "oshidashi", 30), row(Division.Makuuchi, "yorikiri", 30),
			row(Division.Makuuchi, "hatakikomi", 20), row(Division.Makuuchi, "uwatenage", 15),
			row(Division.Makuuchi, "okuridashi", 3), row(Division.Makuuchi, "utchari", 2));

	@Test
	@DisplayName("결정기술 → 패배 유형: 공식 분류와 다르게 둔 突き落とし·とったり, 모르는 기술·null은 기타")
	void lossTypeMapping() {
		assertThat(LossType.of("tsukiotoshi")).isEqualTo(LossType.OCHI);
		assertThat(LossType.of("tottari")).isEqualTo(LossType.NAGE);
		assertThat(LossType.of("Yorikiri")).isEqualTo(LossType.YORI);
		assertThat(LossType.of("someNewKimarite")).isEqualTo(LossType.ETC);
		assertThat(LossType.of(null)).isEqualTo(LossType.ETC);
	}

	@Test
	@DisplayName("패배 유형 - 평균보다 크게 높으면 약점, 크게 낮으면 강점, 애매하면 표시 없음")
	void lossTypeVerdicts() {
		// 25패: 고꾸라짐 12(48%, 평균 20) / 밀려서 3(12%, 평균 30) / 잡혀서 10(40%, 평균 30)
		given(torikumiRepository.findLossKimariteByDivision(ID)).willReturn(List.of(
				row(Division.Makuuchi, "hatakikomi", 12),
				row(Division.Makuuchi, "oshidashi", 3),
				row(Division.Makuuchi, "yorikiri", 10)));
		given(torikumiRepository.findKimariteByDivision()).willReturn(MAKUUCHI_LEAGUE);

		LossTypeSummaryDTO result = service.getLossTypes(ID, null, null);

		assertThat(result.totalLosses()).isEqualTo(25);
		assertThat(result.smallSample()).isFalse();
		Map<String, LossTypeSummaryDTO.Row> rows = byName(result);
		assertThat(rows.get(LossType.OCHI.getNameKr()).verdict()).isEqualTo(Verdict.WEAK);
		assertThat(rows.get(LossType.OCHI.getNameKr()).percent()).isEqualTo(48.0);
		assertThat(rows.get(LossType.OCHI.getNameKr()).averagePercent()).isEqualTo(20.0);
		assertThat(rows.get(LossType.OSHI.getNameKr()).verdict()).isEqualTo(Verdict.STRONG);
		assertThat(rows.get(LossType.YORI.getNameKr()).verdict()).isEqualTo(Verdict.NONE); // 1.33배라 약점 아님
		// 던져짐 0건(평균 15%)은 차이는 크지만 평균대로여도 25패 중 3.75건뿐이라 강점으로 보지 않음
		assertThat(rows.get(LossType.NAGE.getNameKr()).verdict()).isEqualTo(Verdict.NONE);
		assertThat(result.rows()).hasSize(LossType.values().length);
		assertThat(result.scaleMax()).isEqualTo(48.0);
	}

	@Test
	@DisplayName("패배 유형 - 약점은 그 유형 패배가 5건 이상일 때만 (비율만 튀는 소수 유형 방지)")
	void weakNeedsMinimumCount() {
		// 20패 중 뒤를 잡힘 4건 = 20% (평균 3%의 6배, +17%p)지만 4건이라 약점 아님
		given(torikumiRepository.findLossKimariteByDivision(ID)).willReturn(List.of(
				row(Division.Makuuchi, "okuridashi", 4),
				row(Division.Makuuchi, "yorikiri", 8),
				row(Division.Makuuchi, "oshidashi", 8)));
		given(torikumiRepository.findKimariteByDivision()).willReturn(MAKUUCHI_LEAGUE);

		LossTypeSummaryDTO result = service.getLossTypes(ID, null, null);

		assertThat(byName(result).get(LossType.OKURI.getNameKr()).verdict()).isEqualTo(Verdict.NONE);
	}

	@Test
	@DisplayName("패배 유형 - 20패 미만이면 표본 적음, 판정하지 않음")
	void smallSampleHasNoVerdict() {
		given(torikumiRepository.findLossKimariteByDivision(ID)).willReturn(List.of(
				row(Division.Makuuchi, "hatakikomi", 10),
				row(Division.Makuuchi, "yorikiri", 2)));
		given(torikumiRepository.findKimariteByDivision()).willReturn(MAKUUCHI_LEAGUE);

		LossTypeSummaryDTO result = service.getLossTypes(ID, null, null);

		assertThat(result.smallSample()).isTrue();
		assertThat(result.rows()).allMatch(r -> r.verdict() == Verdict.NONE);
	}

	@Test
	@DisplayName("패배 유형 - 리그 평균은 이 선수가 진 경기의 디비전 비율로 가중 평균")
	void averageIsWeightedByDivision() {
		// 마쿠우치 10패 + 쥬료 10패. 밀려서 짐 리그 비율: 마쿠우치 20%, 쥬료 40% → 기대 30%
		given(torikumiRepository.findLossKimariteByDivision(ID)).willReturn(List.of(
				row(Division.Makuuchi, "yorikiri", 10),
				row(Division.Juryo, "yorikiri", 10)));
		given(torikumiRepository.findKimariteByDivision()).willReturn(List.of(
				row(Division.Makuuchi, "oshidashi", 20), row(Division.Makuuchi, "yorikiri", 80),
				row(Division.Juryo, "oshidashi", 40), row(Division.Juryo, "yorikiri", 60)));

		LossTypeSummaryDTO result = service.getLossTypes(ID, null, null);

		assertThat(byName(result).get(LossType.OSHI.getNameKr()).averagePercent()).isEqualTo(30.0);
	}

	@Test
	@DisplayName("패배가 없으면 빈 요약")
	void noLosses() {
		given(torikumiRepository.findLossKimariteByDivision(ID)).willReturn(List.of());

		LossTypeSummaryDTO result = service.getLossTypes(ID, null, null);

		assertThat(result.totalLosses()).isZero();
		assertThat(result.rows()).isEmpty();
	}

	@Test
	@DisplayName("상대 조건별 성적 - 계급·체중 차·키 차 구간 집계와 경계값, 리그 평균은 동·서 두 시점 합산")
	void opponentConditions() {
		List<OpponentBoutRow> bouts = new ArrayList<>();
		// 요코즈나 상대 1승 1패, 마에가시라 8(상위) 1승, 마에가시라 9(하위) 1패, 반즈케 없음 1승(계급 집계 제외)
		bouts.add(bout("Yokozuna", null, 25, 0, true));
		bouts.add(bout("Ozeki", null, 20, 5, false));      // 체중 +20 → 20kg 이상 무거움, 키 +5 → 큼
		bouts.add(bout("Maegashira", 8, 0, -4.9, true));   // 체중 0 → 0~20 무거움, 키 -4.9 → 비슷
		bouts.add(bout("Maegashira", 9, -20, -5, false));  // 체중 -20 → 20kg 이상 가벼움, 키 -5 → 작음
		bouts.add(bout(null, null, -0.5, 0, true));        // 체중 -0.5 → 0~20 가벼움
		given(torikumiRepository.findOpponentBouts(ID)).willReturn(bouts);
		// 리그: 동쪽 시점 체중 차 +25인 경기 10번 중 동쪽 7승 → 무거운 상대 7/10, 서쪽 시점(-25) 가벼운 상대 3/10
		given(torikumiRepository.findPhysiqueDiffStats()).willReturn(List.of(physique(25, 0, 10, 7)));

		OpponentConditionDTO result = service.getOpponentConditions(ID);

		assertThat(result.byRank()).extracting(OpponentConditionDTO.Row::labelKr)
				.containsExactly("요코즈나·오제키", "마에가시라 1~8", "마에가시라 9 이하");
		OpponentConditionDTO.Row yokozunaOzeki = result.byRank().get(0);
		assertThat(yokozunaOzeki.bouts()).isEqualTo(2);
		assertThat(yokozunaOzeki.wins()).isEqualTo(1);
		assertThat(yokozunaOzeki.winPercent()).isEqualTo(50.0);
		assertThat(yokozunaOzeki.leagueWinPercent()).isNull();
		assertThat(yokozunaOzeki.smallSample()).isTrue();

		// 체중: [20kg+ 가벼움, 0~20 가벼움, 0~20 무거움, 20kg+ 무거움]
		assertThat(result.byWeight()).extracting(OpponentConditionDTO.Row::bouts).containsExactly(1L, 1L, 1L, 2L);
		assertThat(result.byWeight().get(3).leagueWinPercent()).isEqualTo(70.0);
		assertThat(result.byWeight().get(0).leagueWinPercent()).isEqualTo(30.0);
		assertThat(result.byWeight().get(1).leagueWinPercent()).isNull(); // 리그 표본 없음

		// 키: [작음, 비슷, 큼]
		assertThat(result.byHeight()).extracting(OpponentConditionDTO.Row::bouts).containsExactly(1L, 3L, 1L);
	}

	// ===== helpers =====

	private static Map<String, LossTypeSummaryDTO.Row> byName(LossTypeSummaryDTO dto) {
		return dto.rows().stream().collect(Collectors.toMap(LossTypeSummaryDTO.Row::nameKr, Function.identity()));
	}

	private static DivisionKimariteCountRow row(Division division, String kimarite, long cnt) {
		return new DivisionKimariteCountRow() {
			public Division getDivision() { return division; }
			public String getKimarite() { return kimarite; }
			public Long getCnt() { return cnt; }
		};
	}

	private static OpponentBoutRow bout(String rankName, Integer rankValue, double weightDiff, double heightDiff, boolean win) {
		return new OpponentBoutRow() {
			public String getOppRankName() { return rankName; }
			public Integer getOppRankValue() { return rankValue; }
			public Number getWeightDiff() { return weightDiff; }
			public Number getHeightDiff() { return heightDiff; }
			public Number getWin() { return win ? 1 : 0; }
		};
	}

	private static PhysiqueDiffRow physique(double weightDiff, double heightDiff, long bouts, long eastWins) {
		return new PhysiqueDiffRow() {
			public Number getWeightDiff() { return weightDiff; }
			public Number getHeightDiff() { return heightDiff; }
			public Number getBouts() { return bouts; }
			public Number getEastWins() { return eastWins; }
		};
	}
}
