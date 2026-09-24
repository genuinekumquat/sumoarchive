package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.client.SumoApiClient;
import com.torikumilab.sumoarchive.domain.dto.RosterImportResultDTO;
import com.torikumilab.sumoarchive.domain.dto.sumoapi.SumoApiRikishiDTO;
import com.torikumilab.sumoarchive.domain.dto.sumoapi.SumoApiRikishiPageDTO;
import com.torikumilab.sumoarchive.domain.entity.HeyaEntity;
import com.torikumilab.sumoarchive.domain.entity.RikishiEntity;
import com.torikumilab.sumoarchive.repository.HeyaRepository;
import com.torikumilab.sumoarchive.repository.RikishiRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RosterImportServiceTest {

	@Mock
	private SumoApiClient sumoApiClient;

	@Mock
	private RikishiRepository rikishiRepository;

	@Mock
	private HeyaRepository heyaRepository;

	@InjectMocks
	private RosterImportService rosterImportService;

	@Test
	@DisplayName("기존 리키시는 한글 시코나/뒷이름을 보존한 채 스펙만 갱신(Update)되어야 한다")
	void importRoster_whenExistingRikishi_updatesWithoutOverwritingKoreanNames() {
		// given: 기존 DB에 호쇼류(ID: 662)가 등록되어 있음
		HeyaEntity existingHeya = HeyaEntity.builder()
				.nameEn("Tatsunami")
				.nameKr("타츠나미")
				.nameJp("立浪")
				.build();

		RikishiEntity existingRikishi = RikishiEntity.builder()
				.externalApiId(662)
				.shikonaKr("호쇼류")
				.givenNameKr("토모카즈")
				.shikonaJp("豊昇龍")
				.shikonaEn("Hoshoryu")
				.height(BigDecimal.valueOf(187))
				.weight(BigDecimal.valueOf(140))
				.heyaEntity(existingHeya)
				.build();

		given(heyaRepository.findAll()).willReturn(List.of(existingHeya));
		given(rikishiRepository.findAll()).willReturn(List.of(existingRikishi));

		// API 응답: 몸무게가 145로 증량된 최신 정보 수신
		SumoApiRikishiDTO apiRikishi = new SumoApiRikishiDTO(
				662L,
				"Hoshoryu",
				"豊昇龍 智勝",
				"Tatsunami",
				"Ozeki",
				"1999-05-22",
				"Mongolia, Ulaanbaatar",
				188,
				145,
				"201711",
				null
		);
		SumoApiRikishiPageDTO page = new SumoApiRikishiPageDTO(500, 0, 1, List.of(apiRikishi));
		given(sumoApiClient.getActiveRikishis(500, 0)).willReturn(page);

		// when
		RosterImportResultDTO result = rosterImportService.importRoster();

		// then
		assertThat(result.rikishiUpdated()).isEqualTo(1);
		assertThat(result.rikishiCreated()).isEqualTo(0);
		assertThat(result.heyaCreated()).isEqualTo(0);

		// 중요: 한글 시코나 및 뒷이름이 null로 덮어씌워지지 않고 보존되었는지 검증
		assertThat(existingRikishi.getShikonaKr()).isEqualTo("호쇼류");
		assertThat(existingRikishi.getGivenNameKr()).isEqualTo("토모카즈");

		// 신체 스펙(키, 몸무게)은 최신 데이터로 안전하게 갱신되었는지 검증
		assertThat(existingRikishi.getHeight()).isEqualTo(BigDecimal.valueOf(188));
		assertThat(existingRikishi.getWeight()).isEqualTo(BigDecimal.valueOf(145));

		// 새 엔티티 저장은 호출되지 않음
		verify(rikishiRepository, never()).save(any(RikishiEntity.class));
		verify(heyaRepository, never()).save(any(HeyaEntity.class));
	}

	@Test
	@DisplayName("신규 리키시는 Insert되고 자동 음차가 생성되어야 한다")
	void importRoster_whenNewRikishi_insertsWithInitialTransliteration() {
		// given: DB가 비어있는 상태
		given(heyaRepository.findAll()).willReturn(List.of());
		given(rikishiRepository.findAll()).willReturn(List.of());

		SumoApiRikishiDTO apiRikishi = new SumoApiRikishiDTO(
				999L,
				"Shinsei",
				"新星 太郎",
				"Takasago",
				"Jonokuchi",
				"2005-01-01",
				"Tokyo-to, Edogawa-ku",
				180,
				120,
				"202401",
				null
		);
		SumoApiRikishiPageDTO page = new SumoApiRikishiPageDTO(500, 0, 1, List.of(apiRikishi));
		given(sumoApiClient.getActiveRikishis(500, 0)).willReturn(page);

		HeyaEntity savedHeya = HeyaEntity.builder().nameEn("Takasago").nameKr("타카사고").nameJp("Takasago").build();
		given(heyaRepository.save(any(HeyaEntity.class))).willReturn(savedHeya);

		RikishiEntity savedRikishi = RikishiEntity.builder().externalApiId(999).shikonaEn("Shinsei").build();
		given(rikishiRepository.save(any(RikishiEntity.class))).willReturn(savedRikishi);

		// when
		RosterImportResultDTO result = rosterImportService.importRoster();

		// then
		assertThat(result.rikishiCreated()).isEqualTo(1);
		assertThat(result.rikishiUpdated()).isEqualTo(0);
		assertThat(result.heyaCreated()).isEqualTo(1);

		verify(rikishiRepository, times(1)).save(any(RikishiEntity.class));
		verify(heyaRepository, times(1)).save(any(HeyaEntity.class));
	}
}
