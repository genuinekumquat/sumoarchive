package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.domain.entity.BashoEntity;
import com.torikumilab.sumoarchive.domain.entity.constant.BashoMonth;
import com.torikumilab.sumoarchive.repository.BashoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BashoAutoImportSchedulerTest {

	@Mock
	private BashoRepository bashoRepository;
	@Mock
	private BashoBatchImportService bashoBatchImportService;

	// 2026년 11월 큐슈바쇼: 11/8(일) ~ 11/22(일)
	private final BashoEntity kyushu = basho(1, 2026, BashoMonth.NOV, LocalDate.of(2026, 11, 8), LocalDate.of(2026, 11, 22));

	@Test
	@DisplayName("자동 임포트 대상 기간 - 개막 이틀 전(첫날·둘째 날 대진 발표)부터 종료 다음 날까지")
	void isDue() {
		assertThat(BashoAutoImportScheduler.isDue(kyushu, LocalDate.of(2026, 11, 5))).isFalse();  // 개막 사흘 전
		assertThat(BashoAutoImportScheduler.isDue(kyushu, LocalDate.of(2026, 11, 6))).isTrue();   // 개막 이틀 전(금) - 첫날·둘째 날 대진 발표
		assertThat(BashoAutoImportScheduler.isDue(kyushu, LocalDate.of(2026, 11, 7))).isTrue();   // 개막 전날
		assertThat(BashoAutoImportScheduler.isDue(kyushu, LocalDate.of(2026, 11, 8))).isTrue();   // 첫날
		assertThat(BashoAutoImportScheduler.isDue(kyushu, LocalDate.of(2026, 11, 22))).isTrue();  // 센슈라쿠
		assertThat(BashoAutoImportScheduler.isDue(kyushu, LocalDate.of(2026, 11, 23))).isTrue();  // 다음 날 (결정전·수상)
		assertThat(BashoAutoImportScheduler.isDue(kyushu, LocalDate.of(2026, 11, 24))).isFalse();
	}

	@Test
	@DisplayName("일정이 비어 있는 바쇼는 대상이 아님")
	void isDueWithoutSchedule() {
		BashoEntity noSchedule = basho(2, 2027, BashoMonth.JAN, null, null);
		assertThat(BashoAutoImportScheduler.isDue(noSchedule, LocalDate.of(2027, 1, 15))).isFalse();
	}

	@Test
	@DisplayName("바쇼 기간이면 그 바쇼만 일괄 임포트하고 결과를 남김")
	void runIfDueImportsCurrentBasho() {
		BashoEntity sept = basho(3, 2026, BashoMonth.SEP, LocalDate.of(2026, 9, 13), LocalDate.of(2026, 9, 27));
		given(bashoRepository.findAllByOrderByStartDateDesc()).willReturn(List.of(kyushu, sept));
		given(bashoBatchImportService.importBashoFull(1)).willReturn(new BashoBatchImportService.BashoBatchResult(
				"2026년 11월 큐슈바쇼", 0, 700, 330, 0, 0, 0, 0, List.of("JURYO 3일차 결과 미확정 2경기 (재임포트 시 반영)")));

		BashoAutoImportScheduler scheduler = new BashoAutoImportScheduler(bashoRepository, bashoBatchImportService, true);
		scheduler.runIfDue(LocalDate.of(2026, 11, 10));

		verify(bashoBatchImportService).importBashoFull(1);
		verify(bashoBatchImportService, never()).importBashoFull(3);
		BashoAutoImportScheduler.LastRun run = scheduler.status().lastRun();
		assertThat(run.error()).isNull();
		assertThat(run.torikumiCreated()).isEqualTo(330);
		assertThat(run.warningCount()).isEqualTo(1);
		assertThat(scheduler.status().running()).isFalse();
	}

	@Test
	@DisplayName("바쇼 기간이 아니면 아무것도 하지 않음")
	void runIfDueOutsideBasho() {
		given(bashoRepository.findAllByOrderByStartDateDesc()).willReturn(List.of(kyushu));

		BashoAutoImportScheduler scheduler = new BashoAutoImportScheduler(bashoRepository, bashoBatchImportService, true);
		scheduler.runIfDue(LocalDate.of(2026, 10, 12));

		verify(bashoBatchImportService, never()).importBashoFull(anyInt());
		assertThat(scheduler.status().lastRun()).isNull();
	}

	@Test
	@DisplayName("임포트가 실패해도 오류를 남기고 다음 회차를 막지 않음")
	void runIfDueRecordsError() {
		given(bashoRepository.findAllByOrderByStartDateDesc()).willReturn(List.of(kyushu));
		given(bashoBatchImportService.importBashoFull(1)).willThrow(new IllegalStateException("sumo-api 응답 없음"));

		BashoAutoImportScheduler scheduler = new BashoAutoImportScheduler(bashoRepository, bashoBatchImportService, true);
		scheduler.runIfDue(LocalDate.of(2026, 11, 10));

		assertThat(scheduler.status().lastRun().error()).isEqualTo("sumo-api 응답 없음");
		assertThat(scheduler.status().running()).isFalse();
	}

	@Test
	@DisplayName("꺼져 있으면 스케줄 실행이 아무것도 하지 않음")
	void disabled() {
		BashoAutoImportScheduler scheduler = new BashoAutoImportScheduler(bashoRepository, bashoBatchImportService, false);
		scheduler.runScheduled();

		verify(bashoRepository, never()).findAllByOrderByStartDateDesc();
		assertThat(scheduler.status().enabled()).isFalse();
	}

	private static BashoEntity basho(int id, int year, BashoMonth month, LocalDate start, LocalDate end) {
		BashoEntity b = BashoEntity.builder()
				.bashoYear(year)
				.bashoMonth(month)
				.startDate(start)
				.endDate(end)
				.externalBashoId(String.format("%d%02d", year, month.getMonthValue()))
				.build();
		ReflectionTestUtils.setField(b, "id", id);
		return b;
	}
}
