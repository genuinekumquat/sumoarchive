package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.client.SumoApiClient;
import com.torikumilab.sumoarchive.domain.dto.RosterImportResultDTO;
import com.torikumilab.sumoarchive.domain.dto.sumoapi.SumoApiRikishiDTO;
import com.torikumilab.sumoarchive.domain.dto.sumoapi.SumoApiRikishiPageDTO;
import com.torikumilab.sumoarchive.domain.entity.HeyaEntity;
import com.torikumilab.sumoarchive.domain.entity.RikishiEntity;
import com.torikumilab.sumoarchive.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * sumo-api.com에서 로스터(헤야 + 현역 리키시)를 통째로 가져와 우리 DB를 재구성한다.
 * 옛 더미데이터(수작업 한국어 시코나 포함)는 버리는 게 사용자 확정이라, 임포트 전에
 * 리키시·헤야와 그 하위 데이터를 전부 지운다. 바쇼/반즈케/토리쿠미 임포트는 이후 슬라이스.
 */
@Service
@RequiredArgsConstructor
public class RosterImportService {

	private static final Logger log = LoggerFactory.getLogger(RosterImportService.class);
	private static final int PAGE_SIZE = 500;

	private final SumoApiClient sumoApiClient;

	private final RikishiRepository rikishiRepository;
	private final HeyaRepository heyaRepository;
	private final BashoRepository bashoRepository;
	private final BanzukeRepository banzukeRepository;
	private final TorikumiRepository torikumiRepository;
	private final KinboshiRepository kinboshiRepository;
	private final AwardRepository awardRepository;
	private final CommentRepository commentRepository;
	private final RikishiShikonaHistoryRepository rikishiShikonaHistoryRepository;

	@Transactional
	public RosterImportResultDTO importRoster() {
		List<SumoApiRikishiDTO> apiRikishis = fetchAllActive();
		log.info("[RosterImport] sumo-api 현역 리키시 {}명 수신", apiRikishis.size());

		wipeExisting();

		Map<String, HeyaEntity> heyaByEn = importHeya(apiRikishis);

		List<String> warnings = new ArrayList<>();
		int imported = 0;
		for (SumoApiRikishiDTO api : apiRikishis) {
			try {
				rikishiRepository.save(toEntity(api, heyaByEn, warnings));
				imported++;
			} catch (RuntimeException e) {
				warnings.add("리키시 저장 실패 id=" + api.id() + " (" + api.shikonaEn() + "): " + e.getMessage());
			}
		}

		log.info("[RosterImport] 완료 - 헤야 {}개, 리키시 {}명, 경고 {}건",
				heyaByEn.size(), imported, warnings.size());
		return new RosterImportResultDTO(heyaByEn.size(), imported, warnings);
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

	// ===== 삭제 (FK 자식부터) =====

	private void wipeExisting() {
		commentRepository.deleteAllInBatch();
		kinboshiRepository.deleteAllInBatch();
		torikumiRepository.deleteAllInBatch();
		awardRepository.deleteAllInBatch();
		banzukeRepository.deleteAllInBatch();
		rikishiShikonaHistoryRepository.deleteAllInBatch();
		bashoRepository.deleteAllInBatch();
		// heya <-> rikishi 순환 FK: 대표 오야카타 참조를 끊고 나서 삭제
		heyaRepository.findAll().forEach(h -> h.updateMasterRikishi(null));
		heyaRepository.flush();
		rikishiRepository.deleteAllInBatch();
		heyaRepository.deleteAllInBatch();
	}

	// ===== 헤야 =====

	private Map<String, HeyaEntity> importHeya(List<SumoApiRikishiDTO> apiRikishis) {
		Map<String, HeyaEntity> byEn = new LinkedHashMap<>();
		apiRikishis.stream()
				.map(SumoApiRikishiDTO::heya)
				.filter(Objects::nonNull)
				.map(String::strip)
				.filter(s -> !s.isEmpty())
				.distinct()
				.forEach(en -> byEn.put(en, heyaRepository.save(
						// 한/일명은 NOT NULL이라 로마자 임시값. 후속 슬라이스에서 보강.
						HeyaEntity.builder().nameEn(en).nameKr(en).nameJp(en).build())));
		return byEn;
	}

	// ===== 리키시 =====

	private RikishiEntity toEntity(SumoApiRikishiDTO api, Map<String, HeyaEntity> heyaByEn, List<String> warnings) {
		return RikishiEntity.builder()
				.externalApiId((int) api.id())
				.shikonaJp(firstToken(api.shikonaJp()))
				.shikonaEn(api.shikonaEn())
				.shikonaKr(null) // 한국어 시코나는 별도 작업(관리자 입력/음차)
				.birthdate(parseIsoDate(api.birthDate(), api, "birthDate", warnings))
				.birthplace(api.shusshin())
				.nationality(deriveNationality(api.shusshin()))
				.height(toBigDecimal(api.height()))
				.weight(toBigDecimal(api.weight()))
				.debutDate(parseYyyymm(api.debut(), api, warnings))
				.currentRank(api.currentRank()) // 죽은 캐시 필드지만 저장은 무해
				.heyaEntity(api.heya() == null ? null : heyaByEn.get(api.heya().strip()))
				.isActive(true) // intai=false 조회라 전부 현역
				.build();
	}

	// ===== 파싱 헬퍼 =====

	/** "朝乃山　広暉"(전각공백) / "朝乃山 広暉" / "朝乃山" → 첫 토큰(시코나). */
	private static String firstToken(String s) {
		if (s == null) {
			return null;
		}
		String stripped = s.strip();
		return stripped.isEmpty() ? null : stripped.split("[\\s\\u3000]+", 2)[0];
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
	 * shusshin에서 국적을 대충 뽑는다. 일본 지명은 "...-ken, ...-shi" 꼴이라 "-ken" 포함이면 일본,
	 * 아니면 마지막 콤마 뒤 토큰(국가명)으로 본다. 정확한 매핑은 후속 작업.
	 */
	private static String deriveNationality(String shusshin) {
		if (shusshin == null || shusshin.isBlank()) {
			return null;
		}
		if (shusshin.contains("-ken")) {
			return "일본";
		}
		int comma = shusshin.lastIndexOf(',');
		return comma >= 0 ? shusshin.substring(comma + 1).strip() : shusshin.strip();
	}
}
