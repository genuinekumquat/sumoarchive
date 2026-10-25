package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.domain.dto.sumoapi.SumoApiRankDTO;
import com.torikumilab.sumoarchive.domain.entity.RikishiEntity;
import com.torikumilab.sumoarchive.domain.entity.constant.Division;
import com.torikumilab.sumoarchive.service.BanzukeChangeService.Career;
import com.torikumilab.sumoarchive.service.BanzukeChangeService.SekitoriChange;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BanzukeChangeServiceTest {

	private static final YearMonth CUR = YearMonth.of(2026, 11);

	private static Career career(String firstBanzuke, String firstSekitori, String firstMakuuchi) {
		return new Career(BanzukeChangeService.parse(firstBanzuke), BanzukeChangeService.parse(firstSekitori),
				BanzukeChangeService.parse(firstMakuuchi));
	}

	@Test
	@DisplayName("처음 마쿠우치면 신입막, 전에 마쿠우치였고 직전엔 주료면 재입막, 계속 마쿠우치면 변동 없음")
	void makuuchi() {
		assertThat(BanzukeChangeService.sekitoriChange(Division.Makuuchi, Division.Juryo,
				career("202101", "202501", "202611"), CUR)).isEqualTo(SekitoriChange.NEW_MAKUUCHI);
		assertThat(BanzukeChangeService.sekitoriChange(Division.Makuuchi, Division.Juryo,
				career("200401", "200801", "200811"), CUR)).isEqualTo(SekitoriChange.RETURN_MAKUUCHI);
		assertThat(BanzukeChangeService.sekitoriChange(Division.Makuuchi, Division.Makuuchi,
				career("200401", "200801", "200811"), CUR)).isNull();
	}

	@Test
	@DisplayName("처음 세키토리면 신주료, 마쿠시타에서 돌아오면 재주료, 마쿠우치에서 내려온 건 변동 없음")
	void juryo() {
		assertThat(BanzukeChangeService.sekitoriChange(Division.Juryo, Division.Makushita,
				career("202301", "202611", null), CUR)).isEqualTo(SekitoriChange.NEW_JURYO);
		assertThat(BanzukeChangeService.sekitoriChange(Division.Juryo, Division.Makushita,
				career("201801", "202203", null), CUR)).isEqualTo(SekitoriChange.RETURN_JURYO);
		assertThat(BanzukeChangeService.sekitoriChange(Division.Juryo, null, // 직전 반즈케에 없음(휴장 등)
				career("201801", "202203", null), CUR)).isEqualTo(SekitoriChange.RETURN_JURYO);
		assertThat(BanzukeChangeService.sekitoriChange(Division.Juryo, Division.Makuuchi,
				career("201801", "202203", "202301"), CUR)).isNull();
		assertThat(BanzukeChangeService.sekitoriChange(Division.Makushita, Division.Juryo,
				career("201801", "202203", null), CUR)).isNull();
	}

	@Test
	@DisplayName("처음 오른 바쇼는 sumo-api 이력과 우리 반즈케(2019년~) 중 이른 쪽 - 2019년 전 마쿠우치 베테랑은 재입막")
	void careerUsesEarlierOfApiAndDb() {
		RikishiEntity veteran = RikishiEntity.builder().shikonaEn("Tamawashi").build();
		veteran.applyRankHistory("200403", "200801", "200811");
		Map<Division, LocalDate> db = Map.of(
				Division.Makuuchi, LocalDate.of(2026, 11, 8),
				Division.Juryo, LocalDate.of(2026, 7, 12));

		Career c = BanzukeChangeService.career(veteran, db);

		assertThat(c.firstBanzuke()).isEqualTo(YearMonth.of(2004, 3));
		assertThat(c.firstMakuuchi()).isEqualTo(YearMonth.of(2008, 11));
		assertThat(BanzukeChangeService.sekitoriChange(Division.Makuuchi, Division.Juryo, c, CUR))
				.isEqualTo(SekitoriChange.RETURN_MAKUUCHI);
	}

	@Test
	@DisplayName("이력을 아직 안 받은 새 선수는 우리 반즈케만으로 - 이번이 첫 반즈케면 신제자")
	void careerFromDbOnly() {
		RikishiEntity rookie = RikishiEntity.builder().shikonaEn("Hajime").build();

		Career c = BanzukeChangeService.career(rookie, Map.of(Division.Jonokuchi, LocalDate.of(2026, 11, 8)));

		assertThat(c.firstBanzuke()).isEqualTo(CUR);
		assertThat(c.firstSekitori()).isNull();
		assertThat(c.firstMakuuchi()).isNull();
	}

	@Test
	@DisplayName("sumo-api 계급 이력 - 마에즈모·반즈케 외는 빼고 처음 반즈케·세키토리·마쿠우치 바쇼")
	void firstBashoFromHistory() {
		List<SumoApiRankDTO> history = List.of( // 최신순으로 온다
				new SumoApiRankDTO("202311", "Maegashira 15 East"),
				new SumoApiRankDTO("202309", "Juryo 2 West"),
				new SumoApiRankDTO("202307", "Juryo 10 East"),
				new SumoApiRankDTO("202305", "Makushita 10 East"),
				new SumoApiRankDTO("202303", "Mae-zumo"),
				new SumoApiRankDTO("202301", "Banzuke-gai"));

		RankHistorySyncService.FirstBasho first = RankHistorySyncService.firstBasho(history);

		assertThat(first.banzuke()).isEqualTo("202305");
		assertThat(first.sekitori()).isEqualTo("202307");
		assertThat(first.makuuchi()).isEqualTo("202311");
	}

	@Test
	@DisplayName("계급 문자열의 첫 단어로 디비전 (산야쿠·요코즈나는 마쿠우치)")
	void divisionOf() {
		assertThat(RankHistorySyncService.divisionOf("Yokozuna 1 East")).isEqualTo(Division.Makuuchi);
		assertThat(RankHistorySyncService.divisionOf("Komusubi 2 West")).isEqualTo(Division.Makuuchi);
		assertThat(RankHistorySyncService.divisionOf("Juryo 14 West")).isEqualTo(Division.Juryo);
		assertThat(RankHistorySyncService.divisionOf("Jonokuchi 28 West")).isEqualTo(Division.Jonokuchi);
		assertThat(RankHistorySyncService.divisionOf("Mae-zumo")).isNull();
		assertThat(RankHistorySyncService.divisionOf(null)).isNull();
	}
}
