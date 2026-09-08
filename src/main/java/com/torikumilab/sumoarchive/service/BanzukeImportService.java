package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.client.SumoApiClient;
import com.torikumilab.sumoarchive.domain.dto.BanzukeImportResultDTO;
import com.torikumilab.sumoarchive.domain.dto.sumoapi.SumoApiBanzukeDTO;
import com.torikumilab.sumoarchive.domain.entity.BanzukeEntity;
import com.torikumilab.sumoarchive.domain.entity.BashoEntity;
import com.torikumilab.sumoarchive.domain.entity.RikishiEntity;
import com.torikumilab.sumoarchive.domain.entity.constant.Division;
import com.torikumilab.sumoarchive.domain.entity.constant.RankName;
import com.torikumilab.sumoarchive.domain.entity.constant.Side;
import com.torikumilab.sumoarchive.repository.BanzukeRepository;
import com.torikumilab.sumoarchive.repository.BashoRepository;
import com.torikumilab.sumoarchive.repository.RikishiRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * sumo-api.com에서 특정 바쇼·디비전의 반즈케(番付)를 가져와 upsert한다.
 * 리키시는 {@code externalApiId}로 매칭하고, 우리 DB에 없는 리키시(과거 바쇼면 주로 은퇴자)는
 * 건너뛰고 미매칭 목록에 남긴다. 바쇼는 슬라이스 2에서 임포트돼 {@code externalBashoId}(YYYYMM)가
 * 있어야 조회할 수 있다.
 *
 * <p>API {@code rank} 문자열("Maegashira 5 East")을 우리 스키마 3필드로 쪼갠다:
 * 첫 토큰 = {@link RankName}(enum 이름과 철자 일치), 숫자 토큰 = 계급 내 번호(우리 {@code rankValue}),
 * 꼬리 = {@link Side}. API의 인코딩된 {@code rankValue}(501 등)는 쓰지 않는다 — 관리자 수동 입력이
 * 쓰는 값과 의미가 다르다(우리는 "마에가시라 5"의 5).</p>
 */
@Service
@RequiredArgsConstructor
public class BanzukeImportService {

	private static final Logger log = LoggerFactory.getLogger(BanzukeImportService.class);

	private final SumoApiClient sumoApiClient;
	private final BashoRepository bashoRepository;
	private final BanzukeRepository banzukeRepository;
	private final RikishiRepository rikishiRepository;

	@Transactional
	public BanzukeImportResultDTO importDivision(Integer bashoId, Division division) {
		BashoEntity basho = bashoRepository.findById(bashoId)
				.orElseThrow(() -> new EntityNotFoundException("바쇼를 찾을 수 없습니다. id=" + bashoId));
		String yyyymm = basho.getExternalBashoId();
		if (yyyymm == null || yyyymm.isBlank()) {
			throw new IllegalArgumentException(
					"이 바쇼는 sumo-api에서 임포트된 바쇼가 아닙니다. 바쇼 목록에서 먼저 바쇼 임포트를 실행하세요.");
		}

		SumoApiBanzukeDTO api = sumoApiClient.getBanzuke(yyyymm, division);
		List<SumoApiBanzukeDTO.Entry> entries = merge(api);
		if (entries.isEmpty()) {
			throw new IllegalArgumentException(
					"sumo-api에 " + yyyymm + " " + division + " 반즈케 데이터가 없습니다.");
		}

		int created = 0;
		int updated = 0;
		List<String> unmatched = new ArrayList<>();

		for (SumoApiBanzukeDTO.Entry e : entries) {
			if (e.rikishiID() == null) {
				continue;
			}
			RikishiEntity rikishi = rikishiRepository.findByExternalApiId(e.rikishiID()).orElse(null);
			if (rikishi == null) {
				unmatched.add(label(e) + " (api id=" + e.rikishiID() + ")");
				continue;
			}
			RankName rankName = parseRankName(e.rank());
			if (rankName == null) {
				unmatched.add(label(e) + " (계급 파싱 실패: \"" + e.rank() + "\")");
				continue;
			}
			Integer rankNo = parseRankNumber(e.rank());
			Side side = parseSide(e.side(), e.rank());

			BanzukeEntity existing = banzukeRepository
					.findByBashoEntityIdAndRikishiEntityId(basho.getId(), rikishi.getId())
					.orElse(null);
			if (existing != null) {
				existing.updatePlacement(division, rankName, side, rankNo);
				updated++;
			} else {
				banzukeRepository.save(BanzukeEntity.builder()
						.rikishiEntity(rikishi)
						.bashoEntity(basho)
						.division(division)
						.rankName(rankName)
						.side(side)
						.rankValue(rankNo)
						.build());
				created++;
			}
		}

		log.info("[BanzukeImport] {} {} - 신규 {}, 갱신 {}, 미매칭 {}",
				yyyymm, division, created, updated, unmatched.size());
		return new BanzukeImportResultDTO(bashoLabel(basho), division, created, updated, unmatched);
	}

	// ===== 응답 정규화 =====

	private static List<SumoApiBanzukeDTO.Entry> merge(SumoApiBanzukeDTO api) {
		if (api == null) {
			return List.of();
		}
		return Stream.concat(
						api.east() == null ? Stream.empty() : api.east().stream(),
						api.west() == null ? Stream.empty() : api.west().stream())
				.toList();
	}

	// ===== rank 문자열 파싱 =====

	/** "Maegashira 5 East" → RankName.Maegashira. 첫 토큰이 enum 이름과 그대로 일치한다. */
	private static RankName parseRankName(String rank) {
		if (rank == null || rank.isBlank()) {
			return null;
		}
		String head = rank.strip().split("\\s+", 2)[0];
		try {
			return RankName.valueOf(head);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	/** "Maegashira 5 East" → 5. 번호가 없으면(요코즈나 1명뿐인 바쇼 등) null. */
	private static Integer parseRankNumber(String rank) {
		if (rank == null) {
			return null;
		}
		for (String tok : rank.strip().split("\\s+")) {
			if (!tok.isEmpty() && tok.chars().allMatch(Character::isDigit)) {
				return Integer.valueOf(tok);
			}
		}
		return null;
	}

	/** side 필드("East"/"West") 우선, 없으면 rank 문자열 꼬리에서. 판별 실패 시 EAST. */
	private static Side parseSide(String side, String rank) {
		String s = side != null ? side : rank;
		if (s != null && s.toLowerCase().contains("west")) {
			return Side.WEST;
		}
		return Side.EAST;
	}

	private static String label(SumoApiBanzukeDTO.Entry e) {
		return e.shikonaEn() != null && !e.shikonaEn().isBlank() ? e.shikonaEn() : "(이름 미상)";
	}

	private static String bashoLabel(BashoEntity basho) {
		return basho.getBashoYear() + "년 " + basho.getBashoMonth().getMonthValue() + "월 "
				+ basho.getBashoMonth().getDisplayNameKr();
	}
}
