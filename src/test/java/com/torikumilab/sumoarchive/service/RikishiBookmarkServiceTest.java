package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.domain.dto.BookmarkPageDTO;
import com.torikumilab.sumoarchive.domain.dto.RikishiBookmarkCardDTO;
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
import com.torikumilab.sumoarchive.repository.RikishiRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RikishiBookmarkServiceTest {

	@Mock
	private RikishiRepository rikishiRepository;
	@Mock
	private BashoRepository bashoRepository;
	@Mock
	private BanzukeRepository banzukeRepository;
	@Mock
	private TorikumiRepository torikumiRepository;
	@Mock
	private ScheduledBoutRepository scheduledBoutRepository;
	@Mock
	private HighestRankService highestRankService;
	@InjectMocks
	private RikishiBookmarkService service;

	// 2026년 11월 큐슈바쇼 11/8 ~ 11/22
	private final BashoEntity kyushu = withId(BashoEntity.builder().bashoYear(2026).bashoMonth(BashoMonth.NOV)
			.startDate(LocalDate.of(2026, 11, 8)).endDate(LocalDate.of(2026, 11, 22)).externalBashoId("202611").build(), 10);
	private final RikishiEntity onosato = withId(RikishiEntity.builder().shikonaKr("오노사토").shikonaJp("大の里").build(), 1);
	private final RikishiEntity hoshoryu = withId(RikishiEntity.builder().shikonaKr("호쇼류").shikonaJp("豊昇龍").build(), 2);
	private final RikishiEntity kirishima = withId(RikishiEntity.builder().shikonaKr("키리시마").shikonaJp("霧島").build(), 3);
	private final RikishiEntity retired = withId(RikishiEntity.builder().shikonaKr("하쿠호").build(), 4);

	@Test
	@DisplayName("바쇼 상태 - 개막 전·진행 중(시작일~종료일)·종료")
	void bashoStatus() {
		assertThat(RikishiBookmarkService.bashoInfo(kyushu, LocalDate.of(2026, 11, 7), null, null).status()).isEqualTo(BookmarkPageDTO.Status.UPCOMING);
		assertThat(RikishiBookmarkService.bashoInfo(kyushu, LocalDate.of(2026, 11, 8), null, null).status()).isEqualTo(BookmarkPageDTO.Status.IN_PROGRESS);
		assertThat(RikishiBookmarkService.bashoInfo(kyushu, LocalDate.of(2026, 11, 22), 15, null).status()).isEqualTo(BookmarkPageDTO.Status.IN_PROGRESS);
		assertThat(RikishiBookmarkService.bashoInfo(kyushu, LocalDate.of(2026, 11, 23), 15, null).status()).isEqualTo(BookmarkPageDTO.Status.FINISHED);
		assertThat(RikishiBookmarkService.bashoInfo(kyushu, LocalDate.of(2026, 11, 7), null, null).labelKr()).isEqualTo("2026년 11월 큐슈바쇼");
	}

	@Test
	@DisplayName("대회 중 - 공식 성적이 경기 기록보다 뒤처지면 경기로 세고, 마지막 경기와 ○●□를 붙임. 요청 순서 유지, 은퇴자는 이번 바쇼 없음")
	void inProgress() {
		BanzukeEntity onosatoBz = banzuke(onosato, RankName.Yokozuna, Side.EAST, null);
		onosatoBz.updateRecord(0, 0, 0); // 개막 전에 받은 0승 0패 0휴 그대로
		BanzukeEntity kirishimaBz = banzuke(kirishima, RankName.Ozeki, Side.WEST, null);
		List<TorikumiEntity> bouts = List.of(
				bout(1, onosato, hoshoryu, onosato, ResultType.NORMAL, "yorikiri", 101),
				bout(2, kirishima, onosato, kirishima, ResultType.NORMAL, "hatakikomi", 102),
				bout(3, onosato, kirishima, onosato, ResultType.FUZEN, null, 103));
		given(rikishiRepository.findAllById(List.of(4, 1, 3))).willReturn(List.of(onosato, kirishima, retired));
		given(bashoRepository.findLatestWithBanzuke()).willReturn(Optional.of(kyushu));
		BanzukeEntity hoshoryuBz = banzuke(hoshoryu, RankName.Yokozuna, Side.WEST, null);
		given(banzukeRepository.findByBashoEntityIdAndRikishiEntityIdIn(anyInt(), anyList())).willReturn(List.of(onosatoBz, kirishimaBz, hoshoryuBz));
		// 4일째 대진 발표: 키리시마 vs 호쇼류 (오노사토는 아직 없음)
		given(scheduledBoutRepository.findByBashoAndRikishiIn(anyInt(), anyList())).willReturn(List.of(
				ScheduledBoutEntity.builder().bashoEntity(kyushu).day(4).division(Division.Makuuchi)
						.externalId("202611-4-1").eastRikishiEntity(hoshoryu).westRikishiEntity(kirishima).build()));
		given(scheduledBoutRepository.findMinDay(10)).willReturn(4);
		given(torikumiRepository.findRegularByBashoAndRikishiIn(anyInt(), anyList())).willReturn(bouts);
		given(torikumiRepository.findMaxRegularDay(10)).willReturn(3);

		BookmarkPageDTO page = service.getBookmarkPage(List.of(4, 1, 3), LocalDate.of(2026, 11, 10));

		assertThat(page.basho().status()).isEqualTo(BookmarkPageDTO.Status.IN_PROGRESS);
		assertThat(page.basho().lastDay()).isEqualTo(3);
		assertThat(page.basho().nextDay()).isEqualTo(4);
		assertThat(page.cards()).extracting(RikishiBookmarkCardDTO::rikishiId).containsExactly(4, 1, 3);
		assertThat(page.cards().get(0).current()).isNull();

		RikishiBookmarkCardDTO.Current o = page.cards().get(1).current();
		assertThat(o.rankKr()).isEqualTo("동 요코즈나");
		assertThat(o.rankJp()).isEqualTo("東 横綱");
		assertThat(List.of(o.wins(), o.losses())).containsExactly(2, 1);
		assertThat(o.marks()).containsExactly("WIN", "LOSS", "FUSEN_WIN");
		assertThat(o.lastBout()).isEqualTo(new RikishiBookmarkCardDTO.LastBout(3, true, true, "키리시마", "霧島", null, null, 103));

		RikishiBookmarkCardDTO.Current k = page.cards().get(2).current();
		assertThat(k.marks()).containsExactly("WIN", "FUSEN_LOSS");
		assertThat(k.lastBout().win()).isFalse();
		assertThat(k.lastBout().opponentKr()).isEqualTo("오노사토");
		assertThat(k.nextBout()).isEqualTo(new RikishiBookmarkCardDTO.NextBout(4, "호쇼류", "豊昇龍", "서 요코즈나", "西 横綱"));
		assertThat(o.nextBout()).isNull();
	}

	@Test
	@DisplayName("끝난 바쇼 - 공식 성적(휴 포함)을 그대로 씀")
	void finishedUsesOfficialRecord() {
		BanzukeEntity bz = banzuke(onosato, RankName.Maegashira, Side.EAST, 1);
		bz.updateRecord(8, 4, 3);
		RikishiBookmarkCardDTO.Current c = RikishiBookmarkService.current(1,
				bz, List.of(bout(1, onosato, hoshoryu, hoshoryu, ResultType.NORMAL, "oshidashi", 201)), null);
		assertThat(List.of(c.wins(), c.losses(), c.absences())).containsExactly(8, 4, 3);
		assertThat(c.rankKr()).isEqualTo("동 마에가시라필두");
		assertThat(c.lastBout().kimariteKr()).isNotBlank();
	}

	@Test
	@DisplayName("즐겨찾기 수 상한 50, 중복 id는 한 번만")
	void limitsIds() {
		given(rikishiRepository.findAllById(anyList())).willReturn(List.of());
		given(bashoRepository.findLatestWithBanzuke()).willReturn(Optional.empty());
		List<Integer> many = new java.util.ArrayList<>();
		for (int i = 0; i < 80; i++) many.add(i % 60);

		BookmarkPageDTO page = service.getBookmarkPage(many, LocalDate.of(2026, 11, 10));

		assertThat(page.basho()).isNull();
		org.mockito.Mockito.verify(rikishiRepository).findAllById(org.mockito.ArgumentMatchers.argThat(
				(List<Integer> ids) -> ids.size() == RikishiBookmarkService.MAX_IDS && ids.stream().distinct().count() == ids.size()));
	}

	private BanzukeEntity banzuke(RikishiEntity r, RankName rank, Side side, Integer value) {
		return BanzukeEntity.builder().rikishiEntity(r).bashoEntity(kyushu).division(Division.Makuuchi)
				.rankName(rank).side(side).rankValue(value).build();
	}

	private TorikumiEntity bout(int day, RikishiEntity east, RikishiEntity west, RikishiEntity winner,
								ResultType type, String kimarite, int id) {
		return withId(TorikumiEntity.builder().bashoEntity(kyushu).day(day).division(Division.Makuuchi)
				.eastRikishiEntity(east).westRikishiEntity(west).winnerRikishiEntity(winner)
				.loserRikishiEntity(winner == east ? west : east).resultType(type).kimarite(kimarite).build(), id);
	}

	private static <T> T withId(T entity, int id) {
		ReflectionTestUtils.setField(entity, "id", id);
		return entity;
	}
}
