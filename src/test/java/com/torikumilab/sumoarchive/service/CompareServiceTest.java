package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.domain.entity.BashoEntity;
import com.torikumilab.sumoarchive.domain.entity.constant.BashoMonth;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CompareServiceTest {

	// 최신부터: 2026 큐슈(11/8~11/22) … 2025 나고야
	private final List<BashoEntity> all = List.of(
			basho(2026, BashoMonth.NOV, LocalDate.of(2026, 11, 8)),
			basho(2026, BashoMonth.SEP, LocalDate.of(2026, 9, 13)),
			basho(2026, BashoMonth.JUL, LocalDate.of(2026, 7, 12)),
			basho(2026, BashoMonth.MAY, LocalDate.of(2026, 5, 10)),
			basho(2026, BashoMonth.MAR, LocalDate.of(2026, 3, 8)),
			basho(2026, BashoMonth.JAN, LocalDate.of(2026, 1, 11)),
			basho(2025, BashoMonth.NOV, LocalDate.of(2025, 11, 9)),
			basho(2025, BashoMonth.SEP, LocalDate.of(2025, 9, 14)),
			basho(2025, BashoMonth.JUL, LocalDate.of(2025, 7, 13)));

	@Test
	@DisplayName("개막 전 바쇼는 빼고, 끝난 최근 6바쇼 (큐슈 반즈케 발표 후 → 아키 ~ 작년 큐슈)")
	void beforeOpening() {
		List<BashoEntity> picked = CompareService.pickBasho(all, LocalDate.of(2026, 10, 27));

		assertThat(picked).extracting(BashoEntity::getStartDate).containsExactly(
				LocalDate.of(2026, 9, 13), LocalDate.of(2026, 7, 12), LocalDate.of(2026, 5, 10),
				LocalDate.of(2026, 3, 8), LocalDate.of(2026, 1, 11), LocalDate.of(2025, 11, 9));
	}

	@Test
	@DisplayName("진행 중인 바쇼는 맨 위에 하나 더 붙고, 끝난 6바쇼는 그대로")
	void inProgress() {
		List<BashoEntity> picked = CompareService.pickBasho(all, LocalDate.of(2026, 11, 15));

		assertThat(picked).hasSize(7);
		assertThat(picked.get(0).getStartDate()).isEqualTo(LocalDate.of(2026, 11, 8));
		assertThat(picked.get(6).getStartDate()).isEqualTo(LocalDate.of(2025, 11, 9));
	}

	@Test
	@DisplayName("센슈라쿠 다음 날부터는 끝난 바쇼로 센다")
	void dayAfterSenshuraku() {
		List<BashoEntity> picked = CompareService.pickBasho(all, LocalDate.of(2026, 11, 23));

		assertThat(picked).hasSize(6);
		assertThat(picked.get(0).getStartDate()).isEqualTo(LocalDate.of(2026, 11, 8));
		assertThat(picked.get(5).getStartDate()).isEqualTo(LocalDate.of(2026, 1, 11));
	}

	private static BashoEntity basho(int year, BashoMonth month, LocalDate start) {
		return BashoEntity.builder().bashoYear(year).bashoMonth(month).startDate(start).build();
	}
}
