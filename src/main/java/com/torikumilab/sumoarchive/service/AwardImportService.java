package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.client.SumoApiClient;
import com.torikumilab.sumoarchive.domain.dto.AwardImportResultDTO;
import com.torikumilab.sumoarchive.domain.dto.sumoapi.SumoApiBashoDTO;
import com.torikumilab.sumoarchive.domain.entity.AwardEntity;
import com.torikumilab.sumoarchive.domain.entity.BanzukeEntity;
import com.torikumilab.sumoarchive.domain.entity.BashoEntity;
import com.torikumilab.sumoarchive.domain.entity.KinboshiEntity;
import com.torikumilab.sumoarchive.domain.entity.RikishiEntity;
import com.torikumilab.sumoarchive.domain.entity.TorikumiEntity;
import com.torikumilab.sumoarchive.domain.entity.constant.AwardType;
import com.torikumilab.sumoarchive.domain.entity.constant.Division;
import com.torikumilab.sumoarchive.domain.entity.constant.RankName;
import com.torikumilab.sumoarchive.repository.AwardRepository;
import com.torikumilab.sumoarchive.repository.BanzukeRepository;
import com.torikumilab.sumoarchive.repository.BashoRepository;
import com.torikumilab.sumoarchive.repository.KinboshiRepository;
import com.torikumilab.sumoarchive.repository.RikishiRepository;
import com.torikumilab.sumoarchive.repository.TorikumiRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 한 바쇼의 우승(유쇼)·삼상(산쇼)·킨보시(金星)를 채운다.
 *
 * <ul>
 *   <li><b>우승·삼상</b>: sumo-api {@code /api/basho/{YYYYMM}}의 {@code yusho[]} / {@code specialPrizes[]}.
 *       6개 디비전 우승과 삼상(수훈·감투·기능, 마쿠우치 전용)을 모두 담는다.</li>
 *   <li><b>킨보시</b>: 우리 DB의 반즈케·토리쿠미에서 파생 — 그 바쇼 반즈케에서 <b>마에가시라</b>였던
 *       선수가 <b>요코즈나</b>를 정규 대전에서 이긴(불계승 제외) 경기.</li>
 * </ul>
 *
 * <p>모두 idempotent: 이미 있는 우승/삼상/킨보시는 건너뛰고, 재실행하면 빠진 것만 추가한다.
 * 리키시는 {@code externalApiId}로 매칭하고, 우리 DB에 없으면 건너뛰어 목록에 남긴다.</p>
 */
@Service
@RequiredArgsConstructor
public class AwardImportService {

	private static final Logger log = LoggerFactory.getLogger(AwardImportService.class);

	private final SumoApiClient sumoApiClient;
	private final BashoRepository bashoRepository;
	private final RikishiRepository rikishiRepository;
	private final AwardRepository awardRepository;
	private final KinboshiRepository kinboshiRepository;
	private final BanzukeRepository banzukeRepository;
	private final TorikumiRepository torikumiRepository;

	@Transactional
	public AwardImportResultDTO importForBasho(Integer bashoId) {
		BashoEntity basho = bashoRepository.findById(bashoId)
				.orElseThrow(() -> new EntityNotFoundException("바쇼를 찾을 수 없습니다. id=" + bashoId));
		String yyyymm = basho.getExternalBashoId();
		if (yyyymm == null || yyyymm.isBlank()) {
			throw new IllegalArgumentException(
					"이 바쇼는 sumo-api에서 임포트된 바쇼가 아닙니다. 바쇼 목록에서 먼저 바쇼 임포트를 실행하세요.");
		}

		List<String> skipped = new ArrayList<>();
		SumoApiBashoDTO api = sumoApiClient.getBasho(yyyymm);

		int yushoCreated = importYusho(basho, api, skipped);
		int sanshoCreated = importSansho(basho, api, skipped);
		int kinboshiCreated = deriveKinboshi(basho);

		log.info("[AwardImport] {} - 우승 {}, 삼상 {}, 킨보시 {}, 스킵 {}",
				yyyymm, yushoCreated, sanshoCreated, kinboshiCreated, skipped.size());
		return new AwardImportResultDTO(
				bashoLabel(basho), yushoCreated, sanshoCreated, kinboshiCreated, skipped);
	}

	// ===== 우승 =====

	private int importYusho(BashoEntity basho, SumoApiBashoDTO api, List<String> skipped) {
		if (api == null || api.yusho() == null) {
			return 0;
		}
		int created = 0;
		for (SumoApiBashoDTO.Honor h : api.yusho()) {
			Division division = parseDivision(h.type());
			if (division == null) {
				skipped.add("우승: 알 수 없는 디비전 \"" + h.type() + "\" (" + label(h) + ")");
				continue;
			}
			RikishiEntity rikishi = findRikishi(h);
			if (rikishi == null) {
				skipped.add("우승 " + division + ": 미매칭 " + label(h));
				continue;
			}
			if (saveAwardIfAbsent(rikishi, basho, division, AwardType.YUSHO)) {
				created++;
			}
		}
		return created;
	}

	// ===== 삼상 (마쿠우치 전용) =====

	private int importSansho(BashoEntity basho, SumoApiBashoDTO api, List<String> skipped) {
		if (api == null || api.specialPrizes() == null) {
			return 0;
		}
		int created = 0;
		for (SumoApiBashoDTO.Honor h : api.specialPrizes()) {
			AwardType type = parseSansho(h.type());
			if (type == null) {
				skipped.add("삼상: 알 수 없는 종류 \"" + h.type() + "\" (" + label(h) + ")");
				continue;
			}
			RikishiEntity rikishi = findRikishi(h);
			if (rikishi == null) {
				skipped.add("삼상 " + h.type() + ": 미매칭 " + label(h));
				continue;
			}
			// 삼상은 항상 마쿠우치. uq_award에 division이 필요해 Makuuchi로 고정한다.
			if (saveAwardIfAbsent(rikishi, basho, Division.Makuuchi, type)) {
				created++;
			}
		}
		return created;
	}

	private boolean saveAwardIfAbsent(RikishiEntity rikishi, BashoEntity basho,
									  Division division, AwardType awardType) {
		boolean exists = awardRepository.existsByRikishiEntityIdAndBashoEntityIdAndDivisionAndAwardType(
				rikishi.getId(), basho.getId(), division, awardType);
		if (exists) {
			return false;
		}
		awardRepository.save(AwardEntity.builder()
				.rikishiEntity(rikishi)
				.bashoEntity(basho)
				.division(division)
				.awardType(awardType)
				.build());
		return true;
	}

	// ===== 킨보시 (파생) =====

	private int deriveKinboshi(BashoEntity basho) {
		Map<Integer, RankName> rankByRikishiId = new HashMap<>();
		for (BanzukeEntity b : banzukeRepository.findByBashoEntityIdFetchRikishi(basho.getId())) {
			rankByRikishiId.put(b.getRikishiEntity().getId(), b.getRankName());
		}

		int created = 0;
		for (TorikumiEntity t : torikumiRepository.findRegularByBasho(basho.getId())) {
			RikishiEntity winner = t.getWinnerRikishiEntity();
			RikishiEntity loser = t.getLoserRikishiEntity();
			if (winner == null || loser == null) {
				continue; // 무승부/미결
			}
			RankName winnerRank = rankByRikishiId.get(winner.getId());
			RankName loserRank = rankByRikishiId.get(loser.getId());
			if (winnerRank != RankName.Maegashira || loserRank != RankName.Yokozuna) {
				continue;
			}
			if (kinboshiRepository.existsByTorikumiEntityId(t.getId())) {
				continue;
			}
			kinboshiRepository.save(KinboshiEntity.builder()
					.torikumiEntity(t)
					.winnerRikishiEntity(winner)
					.loserYokozuna(loser)
					.build());
			created++;
		}
		return created;
	}

	// ===== 파싱 헬퍼 =====

	private RikishiEntity findRikishi(SumoApiBashoDTO.Honor h) {
		if (h.rikishiId() == null) {
			return null;
		}
		return rikishiRepository.findByExternalApiId(h.rikishiId()).orElse(null);
	}

	private static Division parseDivision(String type) {
		if (type == null) {
			return null;
		}
		try {
			return Division.valueOf(type.strip());
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	/** "Shukun-sho" / "Kanto-sho" / "Gino-sho" → AwardType. */
	private static AwardType parseSansho(String type) {
		if (type == null) {
			return null;
		}
		return switch (type.strip().toLowerCase()) {
			case "shukun-sho", "shukunsho" -> AwardType.SANSHO_SHUKUN;
			case "kanto-sho", "kantosho" -> AwardType.SANSHO_KANTO;
			case "gino-sho", "ginosho" -> AwardType.SANSHO_GINO;
			default -> null;
		};
	}

	private static String label(SumoApiBashoDTO.Honor h) {
		String name = h.shikonaEn() != null && !h.shikonaEn().isBlank() ? h.shikonaEn() : "(이름 미상)";
		return name + " (api id=" + h.rikishiId() + ")";
	}

	private static String bashoLabel(BashoEntity basho) {
		return basho.getBashoYear() + "년 " + basho.getBashoMonth().getMonthValue() + "월 "
				+ basho.getBashoMonth().getDisplayNameKr();
	}
}
