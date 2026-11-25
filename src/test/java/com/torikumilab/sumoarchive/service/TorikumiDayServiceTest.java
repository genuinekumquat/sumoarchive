package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.domain.dto.TorikumiDayDTO;
import com.torikumilab.sumoarchive.domain.dto.TorikumiDayDTO.DayStatus;
import com.torikumilab.sumoarchive.domain.dto.TorikumiDayDTO.Status;
import com.torikumilab.sumoarchive.domain.entity.BanzukeEntity;
import com.torikumilab.sumoarchive.domain.entity.BashoEntity;
import com.torikumilab.sumoarchive.domain.entity.RikishiEntity;
import com.torikumilab.sumoarchive.domain.entity.RikishiShikonaHistoryEntity;
import com.torikumilab.sumoarchive.domain.entity.ScheduledBoutEntity;
import com.torikumilab.sumoarchive.domain.entity.TorikumiEntity;
import com.torikumilab.sumoarchive.domain.entity.constant.BashoMonth;
import com.torikumilab.sumoarchive.domain.entity.constant.Division;
import com.torikumilab.sumoarchive.domain.entity.constant.RankName;
import com.torikumilab.sumoarchive.domain.entity.constant.ResultType;
import com.torikumilab.sumoarchive.domain.entity.constant.Side;
import com.torikumilab.sumoarchive.repository.BanzukeRepository;
import com.torikumilab.sumoarchive.repository.BashoRepository;
import com.torikumilab.sumoarchive.repository.RikishiShikonaHistoryRepository;
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
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
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
	@Mock
	private RikishiShikonaHistoryRepository rikishiShikonaHistoryRepository;
	@InjectMocks
	private TorikumiDayService service;

	private final BashoEntity kyushu = withId(BashoEntity.builder().bashoYear(2026).bashoMonth(BashoMonth.NOV)
			.startDate(LocalDate.of(2026, 11, 8)).endDate(LocalDate.of(2026, 11, 22)).externalBashoId("202611").build(), 10);
	private final RikishiEntity a = rikishi(1, "오노사토", "大の里");
	private final RikishiEntity b = rikishi(2, "호쇼류", "豊昇龍");
	private final RikishiEntity c = rikishi(3, "키리시마", "霧島");
	private final RikishiEntity d = rikishi(4, "코토자쿠라", "琴櫻");

	@Test
	@DisplayName("기본 일차 - 결과가 있으면 마지막으로 치른 날(결정전 제외), 없으면(개막 전) 첫 예정일")
	void defaultDay() {
		// 바쇼 중: 다음 날 대진이 발표돼 있어도 결과가 있는 날이 먼저
		assertThat(TorikumiDayService.defaultDay(List.of(
				new DayStatus(1, Status.DONE), new DayStatus(2, Status.DONE), new DayStatus(3, Status.SCHEDULED)))).isEqualTo(2);
		// 개막 전: 첫날·둘째 날 대진만 있으면 첫날
		assertThat(TorikumiDayService.defaultDay(List.of(
				new DayStatus(1, Status.SCHEDULED), new DayStatus(2, Status.SCHEDULED)))).isEqualTo(1);
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
		List<TorikumiEntity> done = List.of(
				bout(1, 1, a, b, a, ResultType.NORMAL, "yorikiri", false, 101),
				bout(2, 2, c, a, c, ResultType.NORMAL, "hatakikomi", false, 102));
		given(torikumiRepository.findByBashoAndDivisionFetch(10, Division.Makuuchi)).willReturn(done);
		given(torikumiRepository.findRegularByBashoAndRikishiIn(eq(10), anyCollection())).willReturn(done);
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
		// 예정 경기는 그 경기 전 성적 (1일째 패)
		assertThat(first.east().wins() + "-" + first.east().losses()).isEqualTo("0-1");
		TorikumiDayDTO.Bout second = dto.bouts().get(1);
		assertThat(second.scheduled()).isFalse();
		assertThat(second.winner()).isEqualTo("EAST");
		assertThat(second.torikumiId()).isEqualTo(102);
		assertThat(second.west().rankKr()).isEqualTo("동 요코즈나");
		// 결과가 나온 경기는 그날까지 포함: 이긴 쪽 1-0, 진 쪽은 1일째 승 + 2일째 패 = 1-1
		assertThat(second.east().wins() + "-" + second.east().losses()).isEqualTo("1-0");
		assertThat(second.west().wins() + "-" + second.west().losses()).isEqualTo("1-1");
		assertThat(second.kimariteKr()).isNotBlank();
	}

	@Test
	@DisplayName("성적은 디비전을 가리지 않음 - 마쿠시타 선수가 주료 대진에 올라가도 마쿠시타 경기까지 합쳐서")
	void recordsAcrossDivisions() {
		given(bashoRepository.findLatestWithBanzuke()).willReturn(Optional.of(kyushu));
		// 주료 대진: 15일째 c(주료) vs a(마쿠시타에서 올라옴), a 승
		TorikumiEntity juryoBout = withDivision(bout(15, 1, c, a, a, ResultType.NORMAL, "yorikiri", false, 201), Division.Juryo);
		given(torikumiRepository.findByBashoAndDivisionFetch(10, Division.Juryo)).willReturn(List.of(juryoBout));
		given(scheduledBoutRepository.findByBashoAndDivisionFetch(10, Division.Juryo)).willReturn(List.of());
		// a는 그 전에 마쿠시타에서 5승 1패
		List<TorikumiEntity> aBouts = new java.util.ArrayList<>();
		for (int day = 1; day <= 11; day += 2) {
			aBouts.add(withDivision(bout(day, 1, a, b, day == 9 ? b : a, ResultType.NORMAL, "oshidashi", false, 300 + day), Division.Makushita));
		}
		aBouts.add(juryoBout);
		given(torikumiRepository.findRegularByBashoAndRikishiIn(eq(10), anyCollection())).willReturn(aBouts);

		TorikumiDayDTO.Bout bout = service.getDay(null, Division.Juryo, 15).bouts().get(0);

		assertThat(bout.west().wins() + "-" + bout.west().losses()).isEqualTo("6-1");
		assertThat(bout.east().wins() + "-" + bout.east().losses()).isEqualTo("0-1");
	}

	@Test
	@DisplayName("지난 바쇼는 이름도 그 바쇼 당시 시코나로 (개명 전 이름)")
	void showsShikonaAtThatBasho() {
		RikishiEntity hakunofuji = rikishi(5, "하쿠노후지", "伯乃富士");
		given(bashoRepository.findLatestWithBanzuke()).willReturn(Optional.of(kyushu));
		given(torikumiRepository.findByBashoAndDivisionFetch(10, Division.Makuuchi)).willReturn(List.of(
				bout(1, 1, a, hakunofuji, a, ResultType.NORMAL, "yorikiri", false, 101)));
		given(scheduledBoutRepository.findByBashoAndDivisionFetch(10, Division.Makuuchi)).willReturn(List.of());
		given(rikishiShikonaHistoryRepository.findByRikishiEntityIdIn(anyCollection())).willReturn(List.of(
				RikishiShikonaHistoryEntity.builder().rikishiEntity(hakunofuji).shikonaKr("하쿠오호").shikonaJp("伯桜鵬")
						.validFrom(LocalDate.of(2023, 7, 1)).validTo(LocalDate.of(2026, 12, 31)).build()));

		TorikumiDayDTO.Bout bout = service.getDay(null, Division.Makuuchi, 1).bouts().get(0);

		assertThat(bout.west().nameKr()).isEqualTo("하쿠오호");
		assertThat(bout.west().nameJp()).isEqualTo("伯桜鵬");
		assertThat(bout.east().nameKr()).isEqualTo("오노사토"); // 이력 없는 선수는 지금 이름 그대로
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

	private static TorikumiEntity withDivision(TorikumiEntity t, Division division) {
		ReflectionTestUtils.setField(t, "division", division);
		return t;
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
