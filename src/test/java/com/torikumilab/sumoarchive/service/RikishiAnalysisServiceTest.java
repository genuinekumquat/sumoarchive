package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.domain.dto.BashoGameLogDTO;
import com.torikumilab.sumoarchive.domain.dto.CareerSummaryDTO;
import com.torikumilab.sumoarchive.domain.dto.HeadToHeadBoutDTO;
import com.torikumilab.sumoarchive.domain.dto.HeadToHeadDTO;
import com.torikumilab.sumoarchive.domain.dto.HeadToHeadHighlightsDTO;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
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
		assertThat(LossType.of("harimanage")).isEqualTo(LossType.NAGE);
		assertThat(LossType.of("sokubiotoshi")).isEqualTo(LossType.OCHI);
		assertThat(LossType.of("okuritsuriotoshi")).isEqualTo(LossType.OKURI);
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

	@Test
	@DisplayName("상대전적 요약 - 평소 승률로 예상한 것보다 유독 지거나 이긴 상대만, 부전·3전 미만 제외")
	void headToHeadHighlights() {
		// 본인 70%. 비슷한 강자 A(73%)에게 1승 5패 → 예상 약 2.8승보다 1.8승 적음 → 천적
		//           강자 B(74%)에게 4승 0패 → 예상 약 1.8승보다 2.2승 많음 → 강한 상대
		//           C에게 3승 2패 (예상과 비슷) / D는 2전뿐 / E는 3전 중 1전이 부전이라 실제 2전
		List<HeadToHeadDTO> h2h = List.of(
				h2h(2, "A", 1, 5, 0),
				h2h(3, "B", 4, 0, 0),
				h2h(4, "C", 3, 2, 0),
				h2h(5, "D", 0, 2, 0),
				h2h(6, "E", 0, 2, 1));
		given(torikumiRepository.findWinRates(List.of(ID, 2, 3, 4))).willReturn(List.of(
				winRate(ID, 100, 70), winRate(2, 100, 73), winRate(3, 100, 74), winRate(4, 100, 60)));

		HeadToHeadHighlightsDTO result = service.getHeadToHeadHighlights(ID, h2h, false);

		assertThat(result.tough()).extracting(HeadToHeadHighlightsDTO.Row::opponentShikonaKr).containsExactly("A");
		assertThat(result.tough().get(0).diff()).isEqualTo(-1.8);
		assertThat(result.favorable()).extracting(HeadToHeadHighlightsDTO.Row::opponentShikonaKr).containsExactly("B");
		assertThat(result.favorable().get(0).diff()).isEqualTo(2.2);
	}

	@Test
	@DisplayName("상대전적 요약 - 평범한 선수가 요코즈나에게 3전 전패해도 원래 예상이 낮아 천적이 아님")
	void losingToMuchStrongerIsNotTough() {
		List<HeadToHeadDTO> h2h = List.of(h2h(2, "요코즈나", 0, 3, 0));
		given(torikumiRepository.findWinRates(List.of(ID, 2))).willReturn(List.of(
				winRate(ID, 100, 50), winRate(2, 100, 80)));

		HeadToHeadHighlightsDTO result = service.getHeadToHeadHighlights(ID, h2h, false);

		assertThat(result.isEmpty()).isTrue();
	}

	@Test
	@DisplayName("상대전적 요약 - 세키토리 맞대결이면 평소 승률도 세키토리 경기로 (하위 리그 시절 고승률이 섞이면 천적이 묻힘)")
	void sekitoriMatchupUsesSekitoriRates() {
		// 세키토리 승률은 둘 다 60%로 같은데 1승 5패 → 예상 3승보다 2승 적음 → 천적.
		// 상대의 하위 리그 100전 95승을 섞으면 상대 승률이 77%로 부풀어 예상이 1.9승으로 내려가 천적에서 빠진다.
		List<HeadToHeadDTO> h2h = List.of(h2h(2, "급성장", 1, 5, 0));
		given(torikumiRepository.findWinRates(List.of(ID, 2))).willReturn(List.of(
				winRate(ID, 100, 60), winRate(2, 100, 60), lowerWinRate(2, 100, 95)));

		HeadToHeadHighlightsDTO result = service.getHeadToHeadHighlights(ID, h2h, false);

		assertThat(result.tough()).extracting(HeadToHeadHighlightsDTO.Row::opponentShikonaKr).containsExactly("급성장");
		assertThat(result.tough().get(0).diff()).isEqualTo(-2.0);
	}

	@Test
	@DisplayName("상대전적 요약 - 승수 차이가 1.5에 못 미쳐도 5전 이상·승률 20%p 이상 차이면 포함, 4전 이하는 비율로 안 잡음")
	void rateGapCriterion() {
		// 강자(평소 69%)에게 1승 5패: 예상 승률 약 40.5% → 예상 2.4승, 차이 -1.4승(기준 미달)이지만 승률 차 -24%p → 천적
		// 비슷한 상대에게 1승 3패: 차이 -1승, 승률 차 -25%p지만 4전뿐이라 제외
		List<HeadToHeadDTO> h2h = List.of(
				h2h(2, "강자", 1, 5, 0),
				h2h(3, "비슷", 1, 3, 0));
		given(torikumiRepository.findWinRates(List.of(ID, 2, 3))).willReturn(List.of(
				winRate(ID, 100, 60), winRate(2, 100, 69), winRate(3, 100, 60)));

		HeadToHeadHighlightsDTO result = service.getHeadToHeadHighlights(ID, h2h, false);

		assertThat(result.tough()).extracting(HeadToHeadHighlightsDTO.Row::opponentShikonaKr).containsExactly("강자");
		assertThat(result.tough().get(0).diff()).isEqualTo(-1.4);
	}

	@Test
	@DisplayName("상대전적 요약 - 예상보다 크게 밀려도 실제 전적이 비등하면(7승 8패, 승률 40% 초과) 천적이 아님")
	void evenRecordIsNotTough() {
		// 본인 70% vs 약한 상대 40%: 예상 약 11.6승인데 7승 8패 → 예상 대비 -4.6승이지만 실제로는 비등
		List<HeadToHeadDTO> h2h = List.of(h2h(2, "비등", 7, 8, 0));
		given(torikumiRepository.findWinRates(List.of(ID, 2))).willReturn(List.of(
				winRate(ID, 100, 70), winRate(2, 100, 40)));

		assertThat(service.getHeadToHeadHighlights(ID, h2h, false).isEmpty()).isTrue();
	}

	@Test
	@DisplayName("상대전적 요약 - 순서는 표본을 감안한 승률 상한이 낮은 순 (강자에게 1승 5패 > 약한 상대에게 1승 4패, 3승 16패 > 0승 3패)")
	void toughOrderedByActualRate() {
		// 강자(69%)에게 1승 5패: 예상 대비 -1.4승, 승률 17% / 비슷한 상대(60%)에게 1승 4패: -1.5승, 승률 20%
		List<HeadToHeadDTO> h2h = List.of(
				h2h(2, "약한쪽", 1, 4, 0),
				h2h(3, "강자", 1, 5, 0));
		given(torikumiRepository.findWinRates(List.of(ID, 2, 3))).willReturn(List.of(
				winRate(ID, 100, 60), winRate(2, 100, 60), winRate(3, 100, 69)));

		HeadToHeadHighlightsDTO result = service.getHeadToHeadHighlights(ID, h2h, false);

		assertThat(result.tough()).extracting(HeadToHeadHighlightsDTO.Row::opponentShikonaKr)
				.containsExactly("강자", "약한쪽");

		// 0승 3패(상한 약 56%)보다 3승 16패(상한 약 38%)가 더 확실한 천적
		List<HeadToHeadDTO> h2h2 = List.of(
				h2h(4, "세판", 0, 3, 0),
				h2h(5, "열아홉판", 3, 16, 0));
		given(torikumiRepository.findWinRates(List.of(ID, 4, 5))).willReturn(List.of(
				winRate(ID, 100, 60), winRate(4, 100, 60), winRate(5, 100, 60)));
		assertThat(service.getHeadToHeadHighlights(ID, h2h2, false).tough())
				.extracting(HeadToHeadHighlightsDTO.Row::opponentShikonaKr)
				.containsExactly("열아홉판", "세판");
	}

	@Test
	@DisplayName("상대전적 요약 - 현역 선수 페이지는 은퇴한 상대를 천적 후보에서 뺌 (은퇴 선수 페이지는 전체)")
	void activeOpponentsOnly() {
		HeadToHeadDTO retired = h2h(2, "은퇴", 0, 5, 0);
		retired = new HeadToHeadDTO(retired.opponentId(), retired.opponentShikonaKr(), retired.opponentShikonaJp(),
				retired.opponentShikonaEn(), retired.wins(), retired.losses(), retired.bouts(), false);
		List<HeadToHeadDTO> h2h = List.of(retired, h2h(3, "현역", 0, 5, 0));
		given(torikumiRepository.findWinRates(anyList())).willReturn(List.of(
				winRate(ID, 100, 60), winRate(2, 100, 60), winRate(3, 100, 60)));

		assertThat(service.getHeadToHeadHighlights(ID, h2h, true).tough())
				.extracting(HeadToHeadHighlightsDTO.Row::opponentShikonaKr).containsExactly("현역");
		assertThat(service.getHeadToHeadHighlights(ID, h2h, false).tough())
				.extracting(HeadToHeadHighlightsDTO.Row::opponentShikonaKr).containsExactlyInAnyOrder("은퇴", "현역");
	}

	@Test
	@DisplayName("상대전적 요약 - 1승 차이(2승 3패)는 제외, 현역 페이지는 최근 3년 안에 안 붙은 상대도 제외")
	void marginAndRecency() {
		// 강자(평소 69%)에게 2승 3패도, 1승 차이라 제외
		List<HeadToHeadDTO> oneWinGap = List.of(h2h(2, "한끗", 2, 3, 0));
		// 5년 전에만 0승 5패로 진 상대: 커리어 전체로는 천적, 현역 페이지에선 제외
		HeadToHeadDTO recent = h2h(3, "옛상대", 0, 5, 0);
		List<HeadToHeadBoutDTO> oldBouts = recent.bouts().stream()
				.map(b -> new HeadToHeadBoutDTO(b.bashoLabel(), b.bashoLabelKr(), b.day(), b.win(), b.fusen(),
						b.kimarite(), b.kimariteKr(), b.sekitori(), LocalDate.now().minusYears(5)))
				.toList();
		List<HeadToHeadDTO> old = List.of(new HeadToHeadDTO(3, "옛상대", "옛상대", "옛상대", 0, 5, oldBouts, true));
		given(torikumiRepository.findWinRates(anyList())).willReturn(List.of(
				winRate(ID, 100, 60), winRate(2, 100, 40), winRate(3, 100, 60)));

		assertThat(service.getHeadToHeadHighlights(ID, oneWinGap, false).isEmpty()).isTrue();
		assertThat(service.getHeadToHeadHighlights(ID, old, true).isEmpty()).isTrue();
		assertThat(service.getHeadToHeadHighlights(ID, old, false).tough())
				.extracting(HeadToHeadHighlightsDTO.Row::opponentShikonaKr).containsExactly("옛상대");
	}

	// ===== helpers =====

	/** 부전 경기는 wins/losses와 별개로 fusenLosses만큼 부전패를 덧붙인다. */
	private static HeadToHeadDTO h2h(int opponentId, String name, int wins, int losses, int fusenLosses) {
		List<HeadToHeadBoutDTO> bouts = new ArrayList<>();
		for (int i = 0; i < wins; i++) bouts.add(bout(true, false));
		for (int i = 0; i < losses; i++) bouts.add(bout(false, false));
		for (int i = 0; i < fusenLosses; i++) bouts.add(bout(false, true));
		return new HeadToHeadDTO(opponentId, name, name, name, wins, losses + fusenLosses, bouts, true);
	}

	private static HeadToHeadBoutDTO bout(boolean win, boolean fusen) {
		return new HeadToHeadBoutDTO("2025年01月場所", "2025년 1월 하츠바쇼", 1, win, fusen, "-", "-", true, LocalDate.now());
	}

	/** 세키토리 경기 승률 행 */
	private static TorikumiRepository.WinRateRow winRate(int rikishiId, long bouts, long wins) {
		return winRateRow(rikishiId, 1, bouts, wins);
	}

	/** 마쿠시타 이하 경기 승률 행 */
	private static TorikumiRepository.WinRateRow lowerWinRate(int rikishiId, long bouts, long wins) {
		return winRateRow(rikishiId, 0, bouts, wins);
	}

	private static TorikumiRepository.WinRateRow winRateRow(int rikishiId, int sekitori, long bouts, long wins) {
		return new TorikumiRepository.WinRateRow() {
			public Integer getRikishiId() { return rikishiId; }
			public Number getSekitori() { return sekitori; }
			public Number getBouts() { return bouts; }
			public Number getWins() { return wins; }
		};
	}

	private static Map<String, LossTypeSummaryDTO.Row> byName(LossTypeSummaryDTO dto) {
		return dto.rows().stream().collect(Collectors.toMap(LossTypeSummaryDTO.Row::nameKr, Function.identity()));
	}

	@Test
	@DisplayName("커리어 요약 - 카치코시는 과반 승리(세키토리 8승·하위 4승), 全休는 분모 포함, 진행 중 제외, 두 자릿수·최고 성적은 세키토리만")
	void careerSummary() {
		// 최신 바쇼부터 (getGameLog 순서)
		List<BashoGameLogDTO> gameLog = List.of(
				log(9, 6, 2, 0, Division.Makuuchi, false),   // 진행 중 (6승 2패 시점) - 제외
				log(8, 12, 3, 0, Division.Makuuchi, true),   // 카치코시 + 두 자릿수, 최고 성적 후보(최근)
				log(7, 7, 5, 3, Division.Makuuchi, true),    // 휴장 낀 7승 → 마케코시
				log(6, 0, 0, 15, Division.Juryo, true),      // 全休
				log(5, 12, 3, 0, Division.Juryo, true),      // 12승 동률이지만 더 예전 바쇼
				log(4, 4, 3, 0, Division.Makushita, true),   // 마쿠시타 4승 3패 → 카치코시 (최고 성적 후보는 아님)
				log(3, 3, 4, 0, Division.Sandanme, true),    // 3승 4패 → 마케코시
				log(2, 0, 0, 7, Division.Makushita, true),   // 마쿠시타 全休
				log(1, 0, 0, 0, Division.Juryo, false));     // 적재 전 ("-") - 제외

		CareerSummaryDTO result = service.getCareerSummary(gameLog);

		assertThat(result.basho()).isEqualTo(7);
		assertThat(result.kachikoshi()).isEqualTo(3);
		assertThat(result.kachikoshiPercent()).isEqualTo(43);
		assertThat(result.doubleDigit()).isEqualTo(2);
		assertThat(result.absence()).isEqualTo(3);
		assertThat(result.zenkyu()).isEqualTo(2);
		assertThat(result.best().bashoId()).isEqualTo(8);
	}

	@Test
	@DisplayName("커리어 요약 - 하위 리그 바쇼뿐이면 최고 성적 없음 (하위 리그 7승을 세키토리 성적과 같은 잣대로 보지 않음)")
	void careerSummaryLowerOnly() {
		CareerSummaryDTO result = service.getCareerSummary(List.of(
				log(2, 6, 1, 0, Division.Jonidan, true),
				log(1, 7, 0, 0, Division.Jonokuchi, true)));
		assertThat(result.basho()).isEqualTo(2);
		assertThat(result.kachikoshi()).isEqualTo(2);
		assertThat(result.doubleDigit()).isZero();
		assertThat(result.best()).isNull();
	}

	@Test
	@DisplayName("커리어 요약 - 끝난 바쇼가 없으면 비어 있음, 全休뿐이면 최고 성적 없음")
	void careerSummaryEmpty() {
		assertThat(service.getCareerSummary(List.of(log(1, 3, 1, 0, Division.Juryo, false))).isEmpty()).isTrue();

		CareerSummaryDTO allAbsent = service.getCareerSummary(List.of(log(1, 0, 0, 15, Division.Juryo, true)));
		assertThat(allAbsent.basho()).isEqualTo(1);
		assertThat(allAbsent.kachikoshiPercent()).isZero();
		assertThat(allAbsent.best()).isNull();
	}

	private static BashoGameLogDTO log(int bashoId, int wins, int losses, int absences, Division division, boolean completed) {
		return new BashoGameLogDTO(bashoId, 2026, null, null, null, null, null, null, List.of(),
				division, wins, losses, absences, completed);
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
