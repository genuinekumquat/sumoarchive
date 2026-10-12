package com.torikumilab.sumoarchive.service.stats;

import com.torikumilab.sumoarchive.service.stats.VisitClassifier.Page;
import com.torikumilab.sumoarchive.service.stats.VisitClassifier.PageType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VisitStatsCollectorTest {

	private static final String MOBILE = "Mozilla/5.0 (Linux; Android 10; K) Chrome/143.0.0.0 Mobile Safari/537.36";
	private static final String PC = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/153.0.0.0 Safari/537.36";
	private static final LocalDate DAY = LocalDate.of(2026, 11, 8);

	@Mock
	private JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("같은 날 같은 IP+브라우저는 방문자 1명, 페이지뷰는 매번 - 1분마다 날짜별 합계로 저장")
	@SuppressWarnings("unchecked")
	void countsAndFlushes() {
		VisitStatsCollector collector = new VisitStatsCollector(jdbcTemplate);
		Page main = new Page(PageType.MAIN, 0);
		Page onosato = new Page(PageType.RIKISHI, 1);

		collector.record(main, "1.1.1.1", MOBILE, "dcinside", DAY);
		collector.record(onosato, "1.1.1.1", MOBILE, null, DAY);
		collector.record(onosato, "1.1.1.1", MOBILE, null, DAY);
		collector.record(onosato, "2.2.2.2", PC, "google", DAY);
		collector.flush();

		ArgumentCaptor<List<Object[]>> site = ArgumentCaptor.forClass(List.class);
		verify(jdbcTemplate).batchUpdate(contains("site_daily_stat"), site.capture());
		assertThat(site.getValue()).singleElement()
				.satisfies(row -> assertThat(row).containsExactly(Date.valueOf(DAY), 2L, 4L, 1L)); // 방문자 2, 뷰 4, 모바일 방문자 1

		ArgumentCaptor<List<Object[]>> pages = ArgumentCaptor.forClass(List.class);
		verify(jdbcTemplate).batchUpdate(contains("page_daily_stat"), pages.capture());
		assertThat(pages.getValue()).extracting(r -> r[1] + "/" + r[2] + "=" + r[3])
				.containsExactlyInAnyOrder("MAIN/0=1", "RIKISHI/1=3");

		ArgumentCaptor<List<Object[]>> refs = ArgumentCaptor.forClass(List.class);
		verify(jdbcTemplate).batchUpdate(contains("referrer_daily_stat"), refs.capture());
		assertThat(refs.getValue()).extracting(r -> r[1] + "=" + r[2]).containsExactlyInAnyOrder("dcinside=1", "google=1");

		// 저장한 숫자는 비워져서 다음 flush는 아무것도 쓰지 않음
		collector.flush();
		verify(jdbcTemplate, times(3)).batchUpdate(anyString(), anyList());
	}

	@Test
	@DisplayName("날짜가 바뀌면 같은 사람도 새 방문자")
	@SuppressWarnings("unchecked")
	void newDayNewVisitor() {
		VisitStatsCollector collector = new VisitStatsCollector(jdbcTemplate);
		Page main = new Page(PageType.MAIN, 0);
		collector.record(main, "1.1.1.1", PC, null, DAY);
		collector.record(main, "1.1.1.1", PC, null, DAY.plusDays(1));
		collector.flush();

		ArgumentCaptor<List<Object[]>> site = ArgumentCaptor.forClass(List.class);
		verify(jdbcTemplate).batchUpdate(contains("site_daily_stat"), site.capture());
		assertThat(site.getValue()).extracting(r -> r[0] + ":" + r[1])
				.containsExactly(Date.valueOf(DAY) + ":1", Date.valueOf(DAY.plusDays(1)) + ":1");
		verify(jdbcTemplate, never()).batchUpdate(contains("referrer_daily_stat"), anyList());
	}

	@Test
	@DisplayName("DB 저장이 실패하면 다음 회차에 다시 더함")
	@SuppressWarnings("unchecked")
	void retriesAfterFailure() {
		VisitStatsCollector collector = new VisitStatsCollector(jdbcTemplate);
		lenient().when(jdbcTemplate.batchUpdate(contains("site_daily_stat"), anyList()))
				.thenThrow(new IllegalStateException("DB 끊김"))
				.thenReturn(new int[]{1});

		collector.record(new Page(PageType.MAIN, 0), "1.1.1.1", PC, null, DAY);
		collector.flush(); // 실패
		collector.record(new Page(PageType.MAIN, 0), "3.3.3.3", PC, null, DAY);
		collector.flush(); // 앞의 1명 + 새 1명

		ArgumentCaptor<List<Object[]>> site = ArgumentCaptor.forClass(List.class);
		verify(jdbcTemplate, times(2)).batchUpdate(contains("site_daily_stat"), site.capture());
		assertThat(site.getAllValues().get(1)).singleElement()
				.satisfies(row -> assertThat(row).containsExactly(Date.valueOf(DAY), 2L, 2L, 0L));
	}
}
