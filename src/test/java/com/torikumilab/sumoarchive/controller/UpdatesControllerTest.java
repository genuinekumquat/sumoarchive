package com.torikumilab.sumoarchive.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UpdatesControllerTest {

	private final List<UpdatesController.Entry> entries =
			UpdatesController.load(new ObjectMapper().findAndRegisterModules());

	@Test
	@DisplayName("updates.json - 읽을 수 있고 최신 날짜가 위, 날짜는 겹치지 않음")
	void orderedByDateDesc() {
		assertThat(entries).isNotEmpty();
		List<LocalDate> dates = entries.stream().map(UpdatesController.Entry::date).toList();
		assertThat(dates).doesNotContainNull().doesNotHaveDuplicates()
				.isSortedAccordingTo(Comparator.reverseOrder());
	}

	@Test
	@DisplayName("updates.json - 모든 항목에 한국어가 있고, 빈 문구는 없음 (일본어는 생략 가능)")
	void itemsHaveText() {
		for (UpdatesController.Entry e : entries) {
			assertThat(e.items()).as(e.date().toString()).isNotEmpty();
			for (UpdatesController.Item item : e.items()) {
				assertThat(item.ko()).as(e.date() + " ko").isNotBlank();
				if (item.ja() != null) {
					assertThat(item.ja()).as(e.date() + " ja").isNotBlank();
				}
			}
		}
	}

	private static UpdatesController.Entry entry(String date, UpdatesController.Item... items) {
		return new UpdatesController.Entry(LocalDate.parse(date), List.of(items));
	}

	private static UpdatesController.Item both(String text) {
		return new UpdatesController.Item(text, text);
	}

	@Test
	@DisplayName("월별 묶기 - 최근 달만 펼치고, 최근 달 항목이 3개 미만이면 전달도 펼침")
	void byMonthOpensLatestAndPreviousWhenFew() {
		List<UpdatesController.Entry> sample = List.of(
				entry("2026-11-03", both("a")),
				entry("2026-10-29", both("b"), both("c")),
				entry("2026-10-12", both("d")),
				entry("2026-09-30", both("e")));

		List<UpdatesController.Month> months = UpdatesController.byMonth(sample, UpdatesController.Item::ko);

		assertThat(months).extracting(UpdatesController.Month::month)
				.containsExactly(YearMonth.of(2026, 11), YearMonth.of(2026, 10), YearMonth.of(2026, 9));
		assertThat(months).extracting(UpdatesController.Month::count).containsExactly(1, 3, 1);
		assertThat(months).extracting(UpdatesController.Month::open).containsExactly(true, true, false);
	}

	@Test
	@DisplayName("월별 묶기 - 최근 달 항목이 3개 이상이면 그 달만 펼침")
	void byMonthOpensOnlyLatestWhenEnough() {
		List<UpdatesController.Entry> sample = List.of(
				entry("2026-11-20", both("a"), both("b")),
				entry("2026-11-03", both("c")),
				entry("2026-10-29", both("d")));

		List<UpdatesController.Month> months = UpdatesController.byMonth(sample, UpdatesController.Item::ko);

		assertThat(months).extracting(UpdatesController.Month::open).containsExactly(true, false);
	}

	@Test
	@DisplayName("월별 묶기 - 그 언어 문구가 없는 항목은 빼고 세며, 남는 게 없는 날짜·달은 통째로 뺌")
	void byMonthSkipsItemsWithoutText() {
		List<UpdatesController.Entry> sample = List.of(
				entry("2026-11-03", new UpdatesController.Item("한국어만", null)),
				entry("2026-10-29", both("a"), new UpdatesController.Item("한국어만", null)));

		List<UpdatesController.Month> ja = UpdatesController.byMonth(sample, UpdatesController.Item::ja);

		assertThat(ja).hasSize(1);
		assertThat(ja.get(0).month()).isEqualTo(YearMonth.of(2026, 10));
		assertThat(ja.get(0).count()).isEqualTo(1);
		assertThat(ja.get(0).open()).isTrue();
		assertThat(ja.get(0).entries().get(0).items()).extracting(UpdatesController.Item::ja).containsExactly("a");
	}
}
