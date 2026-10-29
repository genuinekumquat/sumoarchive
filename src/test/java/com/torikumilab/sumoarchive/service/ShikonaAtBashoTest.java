package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.domain.entity.RikishiEntity;
import com.torikumilab.sumoarchive.domain.entity.RikishiShikonaHistoryEntity;
import com.torikumilab.sumoarchive.repository.RikishiShikonaHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ShikonaAtBashoTest {

	@Mock
	private RikishiShikonaHistoryRepository repository;

	@Test
	@DisplayName("바쇼 시작일이 옛 시코나 기간 안이면 옛 이름, 기간 밖이면 null(지금 이름). 양 끝 날짜 포함")
	void at() {
		RikishiEntity hakunofuji = withId(RikishiEntity.builder().shikonaKr("하쿠노후지").shikonaJp("伯乃富士").build(), 649);
		given(repository.findByRikishiEntityIdIn(List.of(649))).willReturn(List.of(
				history(hakunofuji, "오치아이", "落合", LocalDate.of(2023, 1, 1), LocalDate.of(2023, 6, 30)),
				history(hakunofuji, "하쿠오호", "伯桜鵬", LocalDate.of(2023, 7, 1), LocalDate.of(2025, 12, 31)),
				history(hakunofuji, "기간없음", "期間なし", null, null)));

		ShikonaAtBasho names = ShikonaAtBasho.load(repository, List.of(649));

		assertThat(names.at(649, LocalDate.of(2025, 5, 11))).isEqualTo(new ShikonaAtBasho.Name("하쿠오호", "伯桜鵬"));
		assertThat(names.at(649, LocalDate.of(2023, 7, 1)).kr()).isEqualTo("하쿠오호");
		assertThat(names.at(649, LocalDate.of(2023, 6, 30)).kr()).isEqualTo("오치아이");
		assertThat(names.at(649, LocalDate.of(2026, 1, 11))).isNull();   // 개명 뒤
		assertThat(names.at(1, LocalDate.of(2025, 5, 11))).isNull();     // 이력 없는 리키시
		assertThat(names.at(649, null)).isNull();
	}

	@Test
	@DisplayName("리키시가 없으면 조회하지 않는다")
	void emptyIds() {
		assertThat(ShikonaAtBasho.load(repository, List.of()).at(649, LocalDate.of(2025, 5, 11))).isNull();
		then(repository).should(never()).findByRikishiEntityIdIn(anyCollection());
	}

	private static RikishiShikonaHistoryEntity history(RikishiEntity r, String kr, String jp, LocalDate from, LocalDate to) {
		return RikishiShikonaHistoryEntity.builder().rikishiEntity(r).shikonaKr(kr).shikonaJp(jp).validFrom(from).validTo(to).build();
	}

	private static <T> T withId(T entity, int id) {
		ReflectionTestUtils.setField(entity, "id", id);
		return entity;
	}
}
