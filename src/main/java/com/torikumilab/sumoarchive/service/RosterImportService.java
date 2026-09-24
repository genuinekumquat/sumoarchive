package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.client.SumoApiClient;
import com.torikumilab.sumoarchive.domain.dto.RosterImportResultDTO;
import com.torikumilab.sumoarchive.domain.dto.sumoapi.SumoApiRikishiDTO;
import com.torikumilab.sumoarchive.domain.dto.sumoapi.SumoApiRikishiPageDTO;
import com.torikumilab.sumoarchive.domain.entity.HeyaEntity;
import com.torikumilab.sumoarchive.domain.entity.RikishiEntity;
import com.torikumilab.sumoarchive.repository.HeyaRepository;
import com.torikumilab.sumoarchive.repository.RikishiRepository;
import com.torikumilab.sumoarchive.util.OriginDisplayUtil;
import com.torikumilab.sumoarchive.util.ShikonaKrTransliterator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * sumo-api.com에서 현역 로스터(헤야 + 리키시)를 가져와 우리 DB와 안전하게 동기화(Upsert)한다.
 * 기존 데이터를 전체 삭제(wipe)하지 않고, externalApiId(또는 nameEn)를 기준으로
 * 이미 존재하는 데이터는 최신 스펙으로 갱신하고, 새로운 헤야/리키시만 추가한다.
 * 관리자가 검수한 한국어 표기, 수기 입력 데이터, 바쇼/반즈케/댓글 등 연관 데이터는 안전하게 보존된다.
 */
@Service
@RequiredArgsConstructor
public class RosterImportService {

	private static final Logger log = LoggerFactory.getLogger(RosterImportService.class);
	private static final int PAGE_SIZE = 500;

	private final SumoApiClient sumoApiClient;
	private final RikishiRepository rikishiRepository;
	private final HeyaRepository heyaRepository;

	@Transactional
	@CacheEvict(value = {"banzuke", "ichimonStructure"}, allEntries = true)
	public RosterImportResultDTO importRoster() {
		List<SumoApiRikishiDTO> apiRikishis = fetchAllActive();
		log.info("[RosterImport] sumo-api 현역 리키시 {}명 수신", apiRikishis.size());

		// 1. 헤야 동기화 (기존 헤야 보존 + 신규 헤야 추가)
		HeyaImportResult heyaResult = syncHeyas(apiRikishis);

		// 2. 리키시 동기화 (기존 리키시 update + 신규 리키시 insert)
		List<String> warnings = new ArrayList<>();
		int createdCount = 0;
		int updatedCount = 0;

		// 성능 최적화: 기존 리키시들을 externalApiId 키로 맵핑하여 1건당 N+1 쿼리 방지
		Map<Integer, RikishiEntity> existingByExternalId = rikishiRepository.findAll().stream()
				.filter(r -> r.getExternalApiId() != null)
				.collect(Collectors.toMap(RikishiEntity::getExternalApiId, r -> r, (e1, e2) -> e1));

		for (SumoApiRikishiDTO api : apiRikishis) {
			try {
				int apiId = (int) api.id();
				HeyaEntity heya = api.heya() != null
						? heyaResult.heyaByEnMap().get(api.heya().strip().toLowerCase())
						: null;

				RikishiEntity existing = existingByExternalId.get(apiId);
				if (existing != null) {
					// 갱신 (Update): 수동 입력된 한글 시코나/뒷이름/출신지/사진 등은 건드리지 않고 기본 스펙만 갱신
					existing.updateFromApi(
							firstToken(api.shikonaJp()),
							secondToken(api.shikonaJp()),
							api.shikonaEn(),
							parseIsoDate(api.birthDate(), api, "birthDate", warnings),
							api.shusshin(),
							deriveNationality(api.shusshin()),
							toBigDecimal(api.height()),
							toBigDecimal(api.weight()),
							parseYyyymm(api.debut(), api, warnings),
							api.currentRank(),
							heya,
							true
					);
					updatedCount++;
				} else {
					// 신규 추가 (Insert)
					RikishiEntity newRikishi = toEntity(api, heya, warnings);
					RikishiEntity saved = rikishiRepository.save(newRikishi);
					existingByExternalId.put(apiId, saved);
					createdCount++;
				}
			} catch (RuntimeException e) {
				warnings.add("리키시 처리 실패 id=" + api.id() + " (" + api.shikonaEn() + "): " + e.getMessage());
			}
		}

		int totalImported = createdCount + updatedCount;
		log.info("[RosterImport] 동기화 완료 - 헤야 신규 {}개 · 리키시 신규 {}명 / 갱신 {}명 (총 {}명), 경고 {}건",
				heyaResult.createdCount(), createdCount, updatedCount, totalImported, warnings.size());

		return new RosterImportResultDTO(heyaResult.createdCount(), createdCount, updatedCount, totalImported, warnings);
	}

	/**
	 * 특정 리키시 단건 조회 또는 sumo-api 동적 적재.
	 * 반즈케나 토리쿠미 임포트 시 현역 로스터에 없는 은퇴자(예: 2025년 은퇴한 테루노후지 등)가 등장했을 때
	 * 자동으로 단건을 가져와 DB에 안전하게 보존한다.
	 */
	@Transactional
	public RikishiEntity getOrFetchRikishi(Integer apiId) {
		if (apiId == null) {
			return null;
		}
		Optional<RikishiEntity> opt = rikishiRepository.findByExternalApiId(apiId);
		if (opt.isPresent()) {
			return opt.get();
		}

		SumoApiRikishiDTO api = sumoApiClient.getRikishi(apiId);
		if (api == null || api.shikonaEn() == null) {
			return null;
		}

		HeyaEntity heya = null;
		if (api.heya() != null && !api.heya().isBlank()) {
			String heyaName = api.heya().strip();
			heya = heyaRepository.findAll().stream()
					.filter(h -> h.getNameEn() != null && h.getNameEn().equalsIgnoreCase(heyaName))
					.findFirst()
					.orElseGet(() -> {
						String kr = ShikonaKrTransliterator.fromRomaji(heyaName);
						return heyaRepository.save(HeyaEntity.builder()
								.nameEn(heyaName)
								.nameKr(kr != null ? kr : heyaName)
								.nameJp(heyaName)
								.build());
					});
		}

		boolean isActive = (api.intai() == null || api.intai().isBlank());
		LocalDate retired = parseIsoDate(api.intai(), api, "intai", new ArrayList<>());
		String shikonaKr = ShikonaKrTransliterator.fromRomaji(api.shikonaEn());

		RikishiEntity entity = RikishiEntity.builder()
				.externalApiId((int) api.id())
				.shikonaJp(firstToken(api.shikonaJp()))
				.givenNameJp(secondToken(api.shikonaJp()))
				.shikonaEn(api.shikonaEn())
				.shikonaKr(shikonaKr)
				.shikonaKrAuto(shikonaKr != null)
				.birthdate(parseIsoDate(api.birthDate(), api, "birthDate", new ArrayList<>()))
				.birthplace(api.shusshin())
				.nationality(deriveNationality(api.shusshin()))
				.height(toBigDecimal(api.height()))
				.weight(toBigDecimal(api.weight()))
				.debutDate(parseYyyymm(api.debut(), api, new ArrayList<>()))
				.currentRank(api.currentRank())
				.heyaEntity(heya)
				.isActive(isActive)
				.retiredDate(retired)
				.build();

		log.info("[RosterImport] 은퇴/미등록 리키시 단건 자동 동기화: {} (apiId={})", api.shikonaEn(), apiId);
		return rikishiRepository.save(entity);
	}

	// ===== 조회 =====

	private List<SumoApiRikishiDTO> fetchAllActive() {
		List<SumoApiRikishiDTO> all = new ArrayList<>();
		int skip = 0;
		while (true) {
			SumoApiRikishiPageDTO page = sumoApiClient.getActiveRikishis(PAGE_SIZE, skip);
			if (page == null || page.records() == null || page.records().isEmpty()) {
				break;
			}
			all.addAll(page.records());
			skip += PAGE_SIZE;
			if (skip >= page.total()) {
				break;
			}
		}
		return all;
	}

	// ===== 헤야 동기화 (기존 데이터 보존) =====

	private record HeyaImportResult(Map<String, HeyaEntity> heyaByEnMap, int createdCount) {}

	private HeyaImportResult syncHeyas(List<SumoApiRikishiDTO> apiRikishis) {
		Map<String, HeyaEntity> byEn = new HashMap<>();
		for (HeyaEntity h : heyaRepository.findAll()) {
			if (h.getNameEn() != null && !h.getNameEn().isBlank()) {
				byEn.put(h.getNameEn().strip().toLowerCase(), h);
			}
		}

		int created = 0;
		List<String> distinctApiHeyas = apiRikishis.stream()
				.map(SumoApiRikishiDTO::heya)
				.filter(Objects::nonNull)
				.map(String::strip)
				.filter(s -> !s.isEmpty())
				.distinct()
				.toList();

		for (String en : distinctApiHeyas) {
			String key = en.toLowerCase();
			if (!byEn.containsKey(key)) {
				String kr = ShikonaKrTransliterator.fromRomaji(en);
				if (kr == null || kr.isBlank()) {
					kr = en;
				}
				HeyaEntity newHeya = HeyaEntity.builder()
						.nameEn(en)
						.nameKr(kr)
						.nameJp(en) // 일본어 한자명은 관리자 화면에서 추후 검수/입력
						.build();
				HeyaEntity saved = heyaRepository.save(newHeya);
				byEn.put(key, saved);
				created++;
			}
		}
		return new HeyaImportResult(byEn, created);
	}

	// ===== 신규 리키시 생성 =====

	private RikishiEntity toEntity(SumoApiRikishiDTO api, HeyaEntity heya, List<String> warnings) {
		String shikonaKr = ShikonaKrTransliterator.fromRomaji(api.shikonaEn());
		return RikishiEntity.builder()
				.externalApiId((int) api.id())
				.shikonaJp(firstToken(api.shikonaJp()))
				.givenNameJp(secondToken(api.shikonaJp())) // "朝乃山 広暉" → 뒷토큰 "広暉" (없으면 null)
				.shikonaEn(api.shikonaEn())
				.shikonaKr(shikonaKr) // 초기 음차값 자동 설정
				.shikonaKrAuto(shikonaKr != null)
				.birthdate(parseIsoDate(api.birthDate(), api, "birthDate", warnings))
				.birthplace(api.shusshin())
				.nationality(deriveNationality(api.shusshin()))
				.height(toBigDecimal(api.height()))
				.weight(toBigDecimal(api.weight()))
				.debutDate(parseYyyymm(api.debut(), api, warnings))
				.currentRank(api.currentRank())
				.heyaEntity(heya)
				.isActive(true) // intai=false 조회라 전부 현역
				.build();
	}

	// ===== 파싱 헬퍼 =====

	/** "朝乃山 広暉"(전각공백) / "朝乃山 広暉" / "朝乃山" → 첫 토큰(시코나). */
	private static String firstToken(String s) {
		if (s == null) {
			return null;
		}
		String stripped = s.strip();
		return stripped.isEmpty() ? null : stripped.split("[\\s\\u3000]+", 2)[0];
	}

	/** "朝乃山 広暉" → 뒷토큰 "広暉" (뒷이름). 토큰이 하나뿐이면 null. */
	private static String secondToken(String s) {
		if (s == null) {
			return null;
		}
		String[] parts = s.strip().split("[\\s\\u3000]+", 2);
		return parts.length < 2 || parts[1].isBlank() ? null : parts[1].strip();
	}

	private static BigDecimal toBigDecimal(Integer v) {
		return v == null ? null : BigDecimal.valueOf(v);
	}

	private static LocalDate parseIsoDate(String iso, SumoApiRikishiDTO api, String field, List<String> warnings) {
		if (iso == null || iso.length() < 10) {
			return null;
		}
		try {
			return LocalDate.parse(iso.substring(0, 10));
		} catch (RuntimeException e) {
			warnings.add(api.shikonaEn() + " " + field + " 파싱 실패: " + iso);
			return null;
		}
	}

	/** "YYYYMM" → 그 달 1일. 일자 정보가 없어 근사. */
	private static LocalDate parseYyyymm(String yyyymm, SumoApiRikishiDTO api, List<String> warnings) {
		if (yyyymm == null || yyyymm.length() != 6) {
			return null;
		}
		try {
			int y = Integer.parseInt(yyyymm.substring(0, 4));
			int m = Integer.parseInt(yyyymm.substring(4, 6));
			return LocalDate.of(y, m, 1);
		} catch (RuntimeException e) {
			warnings.add(api.shikonaEn() + " debut 파싱 실패: " + yyyymm);
			return null;
		}
	}

	/**
	 * shusshin에서 국적(한국어)을 도출한다.
	 * OriginDisplayUtil의 도도부현/국가명 매핑 로직을 활용한다.
	 */
	private static String deriveNationality(String shusshin) {
		return OriginDisplayUtil.deriveNationality(shusshin);
	}
}
