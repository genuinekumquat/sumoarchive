package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.client.SumoApiClient;
import com.torikumilab.sumoarchive.domain.dto.TorikumiImportResultDTO;
import com.torikumilab.sumoarchive.domain.dto.sumoapi.SumoApiTorikumiDayDTO;
import com.torikumilab.sumoarchive.domain.dto.sumoapi.SumoApiTorikumiDayDTO.Match;
import com.torikumilab.sumoarchive.domain.entity.BashoEntity;
import com.torikumilab.sumoarchive.domain.entity.RikishiEntity;
import com.torikumilab.sumoarchive.domain.entity.ScheduledBoutEntity;
import com.torikumilab.sumoarchive.domain.entity.TorikumiEntity;
import com.torikumilab.sumoarchive.domain.entity.constant.BashoMonth;
import com.torikumilab.sumoarchive.domain.entity.constant.Division;
import com.torikumilab.sumoarchive.repository.BashoRepository;
import com.torikumilab.sumoarchive.repository.RikishiRepository;
import com.torikumilab.sumoarchive.repository.ScheduledBoutRepository;
import com.torikumilab.sumoarchive.repository.TorikumiRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** 경기 임포트의 예정 대진(scheduled_bout) 처리 - 결과 미정이면 예정으로, 결과가 나오거나 안 보이면 예정에서 지움 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TorikumiImportScheduledTest {

	@Mock
	private SumoApiClient sumoApiClient;
	@Mock
	private BashoRepository bashoRepository;
	@Mock
	private TorikumiRepository torikumiRepository;
	@Mock
	private RikishiRepository rikishiRepository;
	@Mock
	private RosterImportService rosterImportService;
	@Mock
	private ScheduledBoutRepository scheduledBoutRepository;
	@InjectMocks
	private TorikumiImportService service;

	private final BashoEntity kyushu = withId(BashoEntity.builder().bashoYear(2026).bashoMonth(BashoMonth.NOV)
			.startDate(LocalDate.of(2026, 11, 8)).endDate(LocalDate.of(2026, 11, 22)).externalBashoId("202611").build(), 10);
	private final RikishiEntity onosato = withId(RikishiEntity.builder().externalApiId(101).shikonaEn("Onosato").build(), 1);
	private final RikishiEntity hoshoryu = withId(RikishiEntity.builder().externalApiId(102).shikonaEn("Hoshoryu").build(), 2);

	@BeforeEach
	void setUp() {
		given(bashoRepository.findById(10)).willReturn(Optional.of(kyushu));
		given(rikishiRepository.findByExternalApiId(101)).willReturn(Optional.of(onosato));
		given(rikishiRepository.findByExternalApiId(102)).willReturn(Optional.of(hoshoryu));
		given(torikumiRepository.findByExternalId(anyString())).willReturn(Optional.empty());
		given(scheduledBoutRepository.findByExternalId(anyString())).willReturn(Optional.empty());
		given(sumoApiClient.getTorikumi(eq("202611"), eq(Division.Makuuchi), anyInt())).willReturn(null);
	}

	@Test
	@DisplayName("결과가 나온 날은 torikumi로, 결과 미정(발표만 된) 날은 예정 대진으로 저장하고, 안 보인 예정은 지움")
	void storesScheduledAndSyncs() {
		day(1, new Match("202611-1-1-101-102", 1, 1, 101, 102, 101, "yorikiri", "Onosato", "Hoshoryu"));
		day(2, new Match("202611-2-1-102-101", 2, 1, 102, 101, 0, "", "Hoshoryu", "Onosato"));
		// 1일째 예정으로 저장돼 있던 것(이제 결과 나옴) + 대진이 바뀌어 사라진 것
		ScheduledBoutEntity decidedNow = scheduled("202611-1-1-101-102", 1);
		ScheduledBoutEntity changed = scheduled("202611-2-9-101-999", 2);
		ScheduledBoutEntity stillScheduled = scheduled("202611-2-1-102-101", 2);
		given(scheduledBoutRepository.findByBashoEntityIdAndDivision(10, Division.Makuuchi))
				.willReturn(List.of(decidedNow, changed, stillScheduled));

		TorikumiImportResultDTO result = service.importDivision(10, Division.Makuuchi);

		assertThat(result.created()).isEqualTo(1);
		verify(torikumiRepository).save(any(TorikumiEntity.class));
		ArgumentCaptor<ScheduledBoutEntity> saved = ArgumentCaptor.forClass(ScheduledBoutEntity.class);
		verify(scheduledBoutRepository).save(saved.capture());
		assertThat(saved.getValue().getDay()).isEqualTo(2);
		assertThat(saved.getValue().getEastRikishiEntity()).isSameAs(hoshoryu);
		assertThat(result.skipped()).anyMatch(s -> s.contains("2일차 예정 대진 1경기"));
		verify(scheduledBoutRepository).delete(decidedNow);
		verify(scheduledBoutRepository).delete(changed);
		verify(scheduledBoutRepository, never()).delete(stillScheduled);
	}

	@Test
	@DisplayName("조회에 실패한 일차가 있으면 예정 대진을 지우지 않음")
	void keepsScheduledWhenFetchFailed() {
		given(sumoApiClient.getTorikumi("202611", Division.Makuuchi, 3)).willThrow(new IllegalStateException("timeout"));
		ScheduledBoutEntity day3 = scheduled("202611-3-1-101-102", 3);
		given(scheduledBoutRepository.findByBashoEntityIdAndDivision(10, Division.Makuuchi)).willReturn(List.of(day3));

		service.importDivision(10, Division.Makuuchi);

		verify(scheduledBoutRepository, never()).delete(any());
	}

	@Test
	@DisplayName("선수를 못 찾은 예정 대진은 저장하지 않고 사유를 남김")
	void skipsUnmatchedScheduled() {
		day(5, new Match("202611-5-1-101-999", 5, 1, 101, 999, 0, null, "Onosato", "Nobody"));

		TorikumiImportResultDTO result = service.importDivision(10, Division.Makuuchi);

		verify(scheduledBoutRepository, never()).save(any());
		assertThat(result.skipped()).anyMatch(s -> s.contains("예정 대진, 미매칭"));
	}

	private void day(int day, Match... matches) {
		given(sumoApiClient.getTorikumi("202611", Division.Makuuchi, day))
				.willReturn(new SumoApiTorikumiDayDTO("2026-11-0" + day, List.of(matches)));
	}

	private ScheduledBoutEntity scheduled(String externalId, int day) {
		return ScheduledBoutEntity.builder().bashoEntity(kyushu).day(day).division(Division.Makuuchi)
				.externalId(externalId).eastRikishiEntity(onosato).westRikishiEntity(hoshoryu).build();
	}

	private static <T> T withId(T entity, int id) {
		ReflectionTestUtils.setField(entity, "id", id);
		return entity;
	}
}
