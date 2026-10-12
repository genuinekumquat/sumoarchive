package com.torikumilab.sumoarchive.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
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
}
