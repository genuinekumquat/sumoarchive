package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.client.SumoApiClient;
import com.torikumilab.sumoarchive.domain.dto.RosterImportResultDTO;
import com.torikumilab.sumoarchive.domain.dto.sumoapi.SumoApiRikishiDTO;
import com.torikumilab.sumoarchive.domain.dto.sumoapi.SumoApiRikishiPageDTO;
import com.torikumilab.sumoarchive.domain.dto.sumoapi.SumoApiShikonaDTO;
import com.torikumilab.sumoarchive.domain.entity.HeyaEntity;
import com.torikumilab.sumoarchive.domain.entity.RikishiEntity;
import com.torikumilab.sumoarchive.domain.entity.RikishiShikonaHistoryEntity;
import com.torikumilab.sumoarchive.repository.HeyaRepository;
import com.torikumilab.sumoarchive.repository.RikishiRepository;
import com.torikumilab.sumoarchive.repository.RikishiShikonaHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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

	@Mock
	private RikishiShikonaHistoryRepository rikishiShikonaHistoryRepository;

	@InjectMocks
	private RosterImportService rosterImportService;

	@Test
	@DisplayName("개명(일본어·영문 둘 다 바뀜)이면 옛 시코나를 이력에 남기고 한글 시코나를 새로 음차한다")
	void importRoster_whenShikonaChanged_recordsHistoryAndRetransliterates() {
		RikishiEntity rikishi = RikishiEntity.builder()
				.externalApiId(118)
				.shikonaKr("칸푸잔")
				.shikonaKrAuto(false)
				.shikonaJp("寒風山")
				.shikonaEn("Kanpuzan")
				.build();
		given(heyaRepository.findAll()).willReturn(List.of());
		given(rikishiRepository.findAll()).willReturn(List.of(rikishi));
		SumoApiRikishiDTO api = new SumoApiRikishiDTO(118L, "Ikawasakura", "井川桜良(いかわさくら)", null,
				"Makushita 41 East", "1993-11-16", "Akita-ken", 184, 142, "201201", null);
		given(sumoApiClient.getActiveRikishis(500, 0))
				.willReturn(new SumoApiRikishiPageDTO(500, 0, 1, List.of(api)));
		// 뒷이름만 바뀐 줄(Kanpuzan Ryota → Kanpuzan)은 같은 시코나 구간, 순서는 최신순으로 와도 된다
		given(sumoApiClient.getShikonaHistory(118)).willReturn(List.of(
				new SumoApiShikonaDTO("202611", "Ikawasakura", "井川桜良(いかわさくら)"),
				new SumoApiShikonaDTO("202503", "Kanpuzan", "寒風山(かんぷうざん)"),
				new SumoApiShikonaDTO("202407", "Kanpuzan Ryota", ""),
				new SumoApiShikonaDTO("202205", "Ryuo Ryota", "")));

		RosterImportResultDTO result = rosterImportService.importRoster();

		ArgumentCaptor<RikishiShikonaHistoryEntity> saved = ArgumentCaptor.forClass(RikishiShikonaHistoryEntity.class);
		verify(rikishiShikonaHistoryRepository).save(saved.capture());
		assertThat(saved.getValue().getShikonaKr()).isEqualTo("칸푸잔");
		assertThat(saved.getValue().getShikonaJp()).isEqualTo("寒風山");
		assertThat(saved.getValue().getValidFrom()).isEqualTo(LocalDate.of(2024, 7, 1));
		assertThat(saved.getValue().getValidTo()).isEqualTo(LocalDate.of(2026, 10, 31));

		assertThat(rikishi.getShikonaJp()).isEqualTo("井川桜良");
		assertThat(rikishi.getShikonaKr()).isNotEqualTo("칸푸잔");
		assertThat(rikishi.isShikonaKrAuto()).isTrue();
		assertThat(result.warnings()).anyMatch(w -> w.startsWith("개명 감지"));
		assertThat(result.shikonaChanges()).singleElement().satisfies(c -> {
			assertThat(c.oldShikonaKr()).isEqualTo("칸푸잔");
			assertThat(c.newShikonaJp()).isEqualTo("井川桜良");
			assertThat(c.newShikonaKr()).isEqualTo(rikishi.getShikonaKr());
		});
	}

	@Test
	@DisplayName("시코나 이력 조회가 실패해도 개명은 기간 없이 기록되고 동기화는 계속된다")
	void importRoster_whenShikonaHistoryFails_recordsWithoutPeriod() {
		RikishiEntity rikishi = RikishiEntity.builder()
				.externalApiId(8882).shikonaKr("이마무라").shikonaJp("今村").shikonaEn("Imamura").build();
		given(heyaRepository.findAll()).willReturn(List.of());
		given(rikishiRepository.findAll()).willReturn(List.of(rikishi));
		SumoApiRikishiDTO api = new SumoApiRikishiDTO(8882L, "Inanoyama", "猪名の山(いなのやま)", null,
				"Makushita 47 West", "2005-09-12", "Hyogo-ken", 181, 131, "202403", null);
		given(sumoApiClient.getActiveRikishis(500, 0))
				.willReturn(new SumoApiRikishiPageDTO(500, 0, 1, List.of(api)));
		given(sumoApiClient.getShikonaHistory(8882)).willThrow(new RuntimeException("timeout"));

		RosterImportResultDTO result = rosterImportService.importRoster();

		ArgumentCaptor<RikishiShikonaHistoryEntity> saved = ArgumentCaptor.forClass(RikishiShikonaHistoryEntity.class);
		verify(rikishiShikonaHistoryRepository).save(saved.capture());
		assertThat(saved.getValue().getValidFrom()).isNull();
		assertThat(saved.getValue().getValidTo()).isNull();
		assertThat(result.rikishiUpdated()).isEqualTo(1);
		assertThat(rikishi.getShikonaJp()).isEqualTo("猪名の山");
	}

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
	@DisplayName("관리자가 고친 출신지(원문)는 로스터 동기화가 덮어쓰지 않는다")
	void importRoster_whenBirthplaceEditedByAdmin_keepsIt() {
		RikishiEntity rikishi = RikishiEntity.builder()
				.externalApiId(662)
				.shikonaEn("Hoshoryu")
				.birthplace("Mongolia, Ulaanbaatar")
				.build();
		saveProfileWithBirthplace(rikishi, "モンゴル・ウランバートル");
		givenRoster(rikishi, "Mongolia, Ulaanbaatar");

		rosterImportService.importRoster();

		assertThat(rikishi.isBirthplaceManual()).isTrue();
		assertThat(rikishi.getBirthplace()).isEqualTo("モンゴル・ウランバートル");
		assertThat(rikishi.getWeight()).isEqualTo(BigDecimal.valueOf(145)); // 다른 스펙은 그대로 갱신
	}

	@Test
	@DisplayName("출신지는 그대로 두고 다른 칸만 저장했거나, 출신지를 비웠으면 동기화가 계속 갱신한다")
	void importRoster_whenBirthplaceNotEdited_keepsUpdating() {
		RikishiEntity unchanged = RikishiEntity.builder()
				.externalApiId(662)
				.shikonaEn("Hoshoryu")
				.birthplace("Mongolia")
				.build();
		saveProfileWithBirthplace(unchanged, "Mongolia");
		givenRoster(unchanged, "Mongolia, Ulaanbaatar");

		rosterImportService.importRoster();

		assertThat(unchanged.isBirthplaceManual()).isFalse();
		assertThat(unchanged.getBirthplace()).isEqualTo("Mongolia, Ulaanbaatar");

		// 직접 고쳤다가 비우면 다시 자동
		RikishiEntity cleared = RikishiEntity.builder().externalApiId(662).shikonaEn("Hoshoryu").build();
		saveProfileWithBirthplace(cleared, "モンゴル");
		saveProfileWithBirthplace(cleared, null);
		assertThat(cleared.isBirthplaceManual()).isFalse();
	}

	/** 관리자 프로필 저장: 출신지만 바꾸고 나머지는 지금 값 그대로 */
	private static void saveProfileWithBirthplace(RikishiEntity r, String birthplace) {
		r.updateProfile(r.getShikonaKr(), r.getShikonaJp(), r.getGivenNameKr(), r.getGivenNameJp(), r.getName(),
				r.getBirthdate(), birthplace, r.getNationality(), r.getHeight(), r.getWeight(), r.getHeyaEntity(),
				r.getHighestRank(), r.getFightingStyle(), r.getDebutDate(), true, r.getRetiredDate(),
				r.getOyakataNameKr(), r.getOyakataNameJp(), r.getPhotoUrl(), r.getOriginKr());
	}

	private void givenRoster(RikishiEntity existing, String shusshin) {
		given(heyaRepository.findAll()).willReturn(List.of());
		given(rikishiRepository.findAll()).willReturn(List.of(existing));
		SumoApiRikishiDTO api = new SumoApiRikishiDTO(662L, "Hoshoryu", "豊昇龍 智勝", null, "Ozeki",
				"1999-05-22", shusshin, 188, 145, "201711", null);
		given(sumoApiClient.getActiveRikishis(500, 0))
				.willReturn(new SumoApiRikishiPageDTO(500, 0, 1, List.of(api)));
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

	@Test
	@DisplayName("관리자가 합친 헤야(개명)의 옛 영문명을 받으면 합친 대상 헤야로 넣는다 - sumo-api가 개명을 늦게 반영하는 경우")
	void importRoster_mergedHeya_resolvesToTarget() {
		HeyaEntity minatogawa = HeyaEntity.builder().nameEn("Minatogawa").nameKr("미나토가와").nameJp("湊川").build();
		ReflectionTestUtils.setField(minatogawa, "id", 118);
		HeyaEntity tokiwayama = HeyaEntity.builder().nameEn("Tokiwayama").nameKr("토키와야마").nameJp("常盤山").build();
		ReflectionTestUtils.setField(tokiwayama, "id", 120);
		tokiwayama.mergeInto(118);

		RikishiEntity takanosho = RikishiEntity.builder()
				.externalApiId(37).shikonaKr("타카노쇼").shikonaEn("Takanosho").heyaEntity(minatogawa).build();
		given(heyaRepository.findAll()).willReturn(List.of(minatogawa, tokiwayama));
		given(rikishiRepository.findAll()).willReturn(List.of(takanosho));
		SumoApiRikishiDTO api = new SumoApiRikishiDTO(37L, "Takanosho", "隆の勝 伸明", "Tokiwayama", "Maegashira 4 East",
				"1994-11-14", "Chiba-ken, Kashiwa-shi", 184, 176, "201003", null);
		given(sumoApiClient.getActiveRikishis(500, 0)).willReturn(new SumoApiRikishiPageDTO(500, 0, 1, List.of(api)));

		RosterImportResultDTO result = rosterImportService.importRoster();

		assertThat(takanosho.getHeyaEntity()).isSameAs(minatogawa); // 옛 이름으로 되돌아가지 않음
		assertThat(result.heyaCreated()).isEqualTo(0);
		verify(heyaRepository, never()).save(any(HeyaEntity.class));
	}

	@Test
	@DisplayName("합치기 따라가기 - 여러 번 합쳐졌으면 끝까지, 잘못 지정된 고리에서도 멈춘다")
	void resolveMerged_followsChain() {
		HeyaEntity a = HeyaEntity.builder().nameEn("A").nameKr("A").nameJp("A").build();
		HeyaEntity b = HeyaEntity.builder().nameEn("B").nameKr("B").nameJp("B").build();
		HeyaEntity c = HeyaEntity.builder().nameEn("C").nameKr("C").nameJp("C").build();
		ReflectionTestUtils.setField(a, "id", 1);
		ReflectionTestUtils.setField(b, "id", 2);
		ReflectionTestUtils.setField(c, "id", 3);
		a.mergeInto(2);
		b.mergeInto(3);
		Map<Integer, HeyaEntity> byId = Map.of(1, a, 2, b, 3, c);

		assertThat(RosterImportService.resolveMerged(a, byId)).isSameAs(c);
		assertThat(RosterImportService.resolveMerged(c, byId)).isSameAs(c);

		c.mergeInto(1); // 고리
		assertThat(RosterImportService.resolveMerged(a, byId)).isNotNull();
	}

	@Test
	@DisplayName("시코나 첫 토큰 - 뒷이름과 끝에 붙은 읽는 법(괄호)은 뗀다")
	void firstToken_stripsGivenNameAndReading() {
		assertThat(RosterImportService.firstToken("朝乃山 広暉")).isEqualTo("朝乃山");
		assertThat(RosterImportService.firstToken("朝乃山　広暉")).isEqualTo("朝乃山");
		assertThat(RosterImportService.firstToken("遠藤(えんどう)")).isEqualTo("遠藤");
		assertThat(RosterImportService.firstToken("志摩ノ海（しまのうみ）")).isEqualTo("志摩ノ海");
		assertThat(RosterImportService.firstToken("Hakuho Sho")).isEqualTo("Hakuho");
		assertThat(RosterImportService.firstToken("  ")).isNull();
		assertThat(RosterImportService.firstToken(null)).isNull();
	}
}
