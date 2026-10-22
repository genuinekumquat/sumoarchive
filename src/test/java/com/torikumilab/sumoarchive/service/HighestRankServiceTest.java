package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.domain.entity.BanzukeEntity;
import com.torikumilab.sumoarchive.domain.entity.BashoEntity;
import com.torikumilab.sumoarchive.domain.entity.constant.BashoMonth;
import com.torikumilab.sumoarchive.domain.entity.constant.Division;
import com.torikumilab.sumoarchive.domain.entity.constant.RankName;
import com.torikumilab.sumoarchive.domain.entity.constant.Side;
import com.torikumilab.sumoarchive.service.HighestRankService.HighestRank;
import com.torikumilab.sumoarchive.service.HighestRankService.Subject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HighestRankServiceTest {

	private static final LocalDate DATA_START = LocalDate.of(2019, 1, 13);
	private static final Subject DEBUT_2020 = new Subject(1, null, YearMonth.of(2020, 3), false);

	@Test
	@DisplayName("가장 높은 지위와 처음 오른 바쇼 - 떨어졌다 다시 올라와도 처음 오른 바쇼")
	void highestAndFirstReached() {
		List<BanzukeEntity> history = List.of(
				banzuke(2023, BashoMonth.JAN, RankName.Makushita, 5),
				banzuke(2023, BashoMonth.MAR, RankName.Juryo, 3),
				banzuke(2023, BashoMonth.MAY, RankName.Juryo, 8),
				banzuke(2024, BashoMonth.JAN, RankName.Sandanme, 10), // 크게 떨어짐
				banzuke(2025, BashoMonth.SEP, RankName.Juryo, 3));    // 같은 지위로 복귀

		HighestRank h = HighestRankService.resolve(DEBUT_2020, history, DATA_START);

		assertThat(h.rank()).isEqualTo("주료3");
		assertThat(h.rankJp()).isEqualTo("十両3枚目");
		assertThat(h.since()).isEqualTo("2023년 3월 하루바쇼");
		assertThat(h.sinceJp()).isEqualTo("2023年03月場所");
		assertThat(h.partial()).isFalse();
	}

	@Test
	@DisplayName("산야쿠 이상은 번호를 보지 않고 처음 오른 바쇼 (세키와케2 → 세키와케1이어도 첫 세키와케)")
	void sanyakuIgnoresNumber() {
		List<BanzukeEntity> history = List.of(
				banzuke(2024, BashoMonth.JAN, RankName.Sekiwake, 2),
				banzuke(2024, BashoMonth.MAR, RankName.Sekiwake, 1),
				banzuke(2023, BashoMonth.NOV, RankName.Maegashira, 1));

		HighestRank h = HighestRankService.resolve(DEBUT_2020, history, DATA_START);

		assertThat(h.rank()).isEqualTo("세키와케");
		assertThat(h.since()).isEqualTo("2024년 1월 하츠바쇼");
	}

	@Test
	@DisplayName("관리자 직접 입력값이 있으면 그 값 (바쇼 없음)")
	void manualWins() {
		Subject manual = new Subject(1, "Ozeki", YearMonth.of(2005, 1), false);

		HighestRank h = HighestRankService.resolve(manual,
				List.of(banzuke(2020, BashoMonth.JAN, RankName.Maegashira, 4)), DATA_START);

		assertThat(h.rank()).isEqualTo("오제키");
		assertThat(h.rankJp()).isEqualTo("大関");
		assertThat(h.since()).isNull();
		assertThat(h.partial()).isFalse();
	}

	@Test
	@DisplayName("기록 시작(2019년 1월) 전 데뷔·데뷔일 모름은 partial, 기록이 없으면 null")
	void partialAndEmpty() {
		List<BanzukeEntity> history = List.of(banzuke(2020, BashoMonth.JAN, RankName.Maegashira, 4));

		assertThat(HighestRankService.resolve(new Subject(1, null, YearMonth.of(2015, 3), false), history, DATA_START).partial()).isTrue();
		assertThat(HighestRankService.resolve(new Subject(1, null, null, false), history, DATA_START).partial()).isTrue();
		assertThat(HighestRankService.resolve(new Subject(1, null, YearMonth.of(2019, 1), false), history, DATA_START).partial()).isFalse();
		assertThat(HighestRankService.resolve(DEBUT_2020, List.of(), DATA_START)).isNull();
	}

	@Test
	@DisplayName("2019년 전 데뷔여도 전체 이력과 대조해 확인됐거나(verified), 요코즈나면 partial이 아니다")
	void verifiedOrYokozunaIsNotPartial() {
		YearMonth debut2010 = YearMonth.of(2010, 1);
		List<BanzukeEntity> maegashira = List.of(banzuke(2020, BashoMonth.JAN, RankName.Maegashira, 4));
		List<BanzukeEntity> yokozuna = List.of(banzuke(2019, BashoMonth.JAN, RankName.Yokozuna, 1));

		assertThat(HighestRankService.resolve(new Subject(1, null, debut2010, true), maegashira, DATA_START).partial()).isFalse();
		assertThat(HighestRankService.resolve(new Subject(1, null, debut2010, false), yokozuna, DATA_START).partial()).isFalse();
		assertThat(HighestRankService.resolve(new Subject(1, null, debut2010, false), maegashira, DATA_START).partial()).isTrue();
	}

	private static BanzukeEntity banzuke(int year, BashoMonth month, RankName rankName, Integer rankValue) {
		BashoEntity basho = BashoEntity.builder()
				.bashoYear(year)
				.bashoMonth(month)
				.startDate(LocalDate.of(year, month.getMonthValue(), 10))
				.build();
		return BanzukeEntity.builder()
				.bashoEntity(basho)
				.division(Division.Makuuchi)
				.rankName(rankName)
				.side(Side.EAST)
				.rankValue(rankValue)
				.build();
	}
}
