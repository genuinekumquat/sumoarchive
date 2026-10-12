package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.domain.dto.TorikumiDayDTO;
import com.torikumilab.sumoarchive.domain.dto.TorikumiDayDTO.DayStatus;
import com.torikumilab.sumoarchive.domain.dto.TorikumiDayDTO.Status;
import com.torikumilab.sumoarchive.domain.entity.BanzukeEntity;
import com.torikumilab.sumoarchive.domain.entity.BashoEntity;
import com.torikumilab.sumoarchive.domain.entity.RikishiEntity;
import com.torikumilab.sumoarchive.domain.entity.ScheduledBoutEntity;
import com.torikumilab.sumoarchive.domain.entity.TorikumiEntity;
import com.torikumilab.sumoarchive.domain.entity.constant.BashoMonth;
import com.torikumilab.sumoarchive.domain.entity.constant.Division;
import com.torikumilab.sumoarchive.domain.entity.constant.RankName;
import com.torikumilab.sumoarchive.domain.entity.constant.ResultType;
import com.torikumilab.sumoarchive.domain.entity.constant.Side;
import com.torikumilab.sumoarchive.repository.BanzukeRepository;
import com.torikumilab.sumoarchive.repository.BashoRepository;
import com.torikumilab.sumoarchive.repository.ScheduledBoutRepository;
import com.torikumilab.sumoarchive.repository.TorikumiRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TorikumiDayServiceTest {

	@Mock
	private BashoRepository bashoRepository;
	@Mock
	private TorikumiRepository torikumiRepository;
	@Mock
	private ScheduledBoutRepository scheduledBoutRepository;
	@Mock
	private BanzukeRepository banzukeRepository;
	@InjectMocks
	private TorikumiDayService service;

	private final BashoEntity kyushu = withId(BashoEntity.builder().bashoYear(2026).bashoMonth(BashoMonth.NOV)
			.startDate(LocalDate.of(2026, 11, 8)).endDate(LocalDate.of(2026, 11, 22)).externalBashoId("202611").build(), 10);
	private final RikishiEntity a = rikishi(1, "오노사토", "大の里");
	private final RikishiEntity b = rikishi(2, "호쇼류", "豊昇龍");
	private final RikishiEntity c = rikishi(3, "키리시마", "霧島");
	private final RikishiEntity d = rikishi(4, "코토자쿠라", "琴櫻");

	@Test
	@DisplayName("기본 일차 - 발표된 예정일이 있으면 그날, 없으면 마지막으로 치른 날(결정전 제외)")
	void defaultDay() {
		assertThat(TorikumiDayService.defaultDay(List.of(
				new DayStatus(1, Status.DONE), new DayStatus(2, Status.DONE), new DayStatus(3, Status.SCHEDULED)))).isEqualTo(3);
		assertThat(TorikumiDayService.defaultDay(List.of(
				new DayStatus(14, Status.DONE), new DayStatus(15, Status.DONE), new DayStatus(16, Status.DONE)))).isEqualTo(15);
		assertThat(TorikumiDayService.defaultDay(List.of())).isNull();
	}

	@Test
	@DisplayName("그 경기 전 성적 - 전날까지 정규 경기만(부전 포함, 결정전 제외)")
	void recordsBefore() {
		List<TorikumiEntity> done = List.of(
				bout(1, 1, a, b, a, ResultType.NORMAL, "yorikiri", false, 101),
				bout(2, 1, a, c, c, ResultType.NORMAL, "hatakikomi", false, 102),
				bout(3, 1, a, d, a, ResultType.FUZEN, null, false, 103),
				bout(16, 1, a, b, b, ResultType.NORMAL, "oshidashi", true, 104));
		Map<Integer, int[]> before3 = TorikumiDayService.recordsBefore(done, 3);
		assertThat(before3.get(1)).containsExactly(1, 1);
		assertThat(before3.get(3)).containsExactly(1, 0);
		assertThat(TorikumiDayService.recordsBefore(done, 16).get(1)).containsExactly(2, 1); // 결정전 자체는 안 셈
	}

	@Test
	@DisplayName("하루치 - 결과와 아직 결과 없는 예정 경기를 경기 순서대로 섞고, 결과가 있는 짝은 예정에서 뺌")
	void mergesDoneAndScheduled() {
		given(bashoRepository.findLatestWithBanzuke()).willReturn(Optional.of(kyushu));
		given(torikumiRepository.findByBashoAndDivisionFetch(10, Division.Makuuchi)).willReturn(List.of(
				bout(1, 1, a, b, a, ResultType.NORMAL, "yorikiri", false, 101),
				bout(2, 2, c, a, c, ResultType.NORMAL, "hatakikomi", false, 102)));
		given(scheduledBoutRepository.findByBashoAndDivisionFetch(10, Division.Makuuchi)).willReturn(List.of(
				scheduled(2, 1, b, d),
				scheduled(2, 2, c, a))); // 결과가 이미 들어온 짝 - 중복으로 나오면 안 됨
		given(banzukeRepository.findByBashoEntityIdAndRikishiEntityIdIn(anyInt(), anyList())).willReturn(List.of(
				BanzukeEntity.builder().rikishiEntity(a).bashoEntity(kyushu).division(Division.Makuuchi)
						.rankName(RankName.Yokozuna).side(Side.EAST).build()));

		TorikumiDayDTO dto = service.getDay(null, Division.Makuuchi, 2);

		assertThat(dto.day()).isEqualTo(2);
		assertThat(dto.bashoLabelKr()).isEqualTo("2026년 11월 큐슈바쇼");
		assertThat(dto.days()).containsExactly(new DayStatus(1, Status.DONE), new DayStatus(2, Status.DONE));
		assertThat(dto.bouts()).hasSize(2);
		TorikumiDayDTO.Bout first = dto.bouts().get(0);
		assertThat(first.scheduled()).isTrue();
		assertThat(first.east().nameKr()).isEqualTo("호쇼류");
		assertThat(first.east().wins() + "-" + first.east().losses()).isEqualTo("0-1");
		TorikumiDayDTO.Bout second = dto.bouts().get(1);
		assertThat(second.scheduled()).isFalse();
		assertThat(second.winner()).isEqualTo("EAST");
		assertThat(second.torikumiId()).isEqualTo(102);
		assertThat(second.west().rankKr()).isEqualTo("동 요코즈나");
		assertThat(second.west().wins() + "-" + second.west().losses()).isEqualTo("1-0");
		assertThat(second.kimariteKr()).isNotBlank();
	}

	@Test
	@DisplayName("없는 일차를 요청하면 기본 일차, 경기가 하나도 없으면 빈 목록")
	void emptyBasho() {
		given(bashoRepository.findLatestWithBanzuke()).willReturn(Optional.of(kyushu));
		given(torikumiRepository.findByBashoAndDivisionFetch(10, Division.Juryo)).willReturn(List.of());
		given(scheduledBoutRepository.findByBashoAndDivisionFetch(10, Division.Juryo)).willReturn(List.of());

		TorikumiDayDTO dto = service.getDay(null, Division.Juryo, 9);

		assertThat(dto.day()).isNull();
		assertThat(dto.days()).isEmpty();
		assertThat(dto.bouts()).isEmpty();
	}

	private TorikumiEntity bout(int day, int matchNo, RikishiEntity east, RikishiEntity west, RikishiEntity winner,
								ResultType type, String kimarite, boolean extra, int id) {
		return withId(TorikumiEntity.builder().bashoEntity(kyushu).day(day).matchNo(matchNo).division(Division.Makuuchi)
				.eastRikishiEntity(east).westRikishiEntity(west).winnerRikishiEntity(winner)
				.loserRikishiEntity(winner == east ? west : east).resultType(type).kimarite(kimarite).isExtraMatch(extra).build(), id);
	}

	private ScheduledBoutEntity scheduled(int day, int matchNo, RikishiEntity east, RikishiEntity west) {
		return ScheduledBoutEntity.builder().bashoEntity(kyushu).day(day).matchNo(matchNo).division(Division.Makuuchi)
				.externalId("s-" + day + "-" + matchNo).eastRikishiEntity(east).westRikishiEntity(west).build();
	}

	private static RikishiEntity rikishi(int id, String kr, String jp) {
		return withId(RikishiEntity.builder().shikonaKr(kr).shikonaJp(jp).build(), id);
	}

	private static <T> T withId(T entity, int id) {
		ReflectionTestUtils.setField(entity, "id", id);
		return entity;
	}
}
