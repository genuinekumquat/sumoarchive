package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.client.SumoApiClient;
import com.torikumilab.sumoarchive.domain.dto.TorikumiImportResultDTO;
import com.torikumilab.sumoarchive.domain.dto.sumoapi.SumoApiTorikumiDayDTO;
import com.torikumilab.sumoarchive.domain.entity.BashoEntity;
import com.torikumilab.sumoarchive.domain.entity.RikishiEntity;
import com.torikumilab.sumoarchive.domain.entity.TorikumiEntity;
import com.torikumilab.sumoarchive.domain.entity.constant.Division;
import com.torikumilab.sumoarchive.domain.entity.constant.ResultType;
import com.torikumilab.sumoarchive.repository.BashoRepository;
import com.torikumilab.sumoarchive.repository.RikishiRepository;
import com.torikumilab.sumoarchive.repository.TorikumiRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * sumo-api.com에서 특정 바쇼·디비전의 15일치 대전을 가져와 upsert한다.
 * 엔드포인트가 하루치({@code /torikumi/{division}/{day}})라 day 1~15를 각각 호출하고,
 * 호출 사이에 짧게 쉬어(throttle) rate limit에 대비한다. 한 일차가 실패해도 나머지는 진행한다.
 *
 * <p>매칭 키는 {@code externalId}("202405-1-1-31-25"), 리키시는 {@code externalApiId}로 찾는다.
 * 동/서 중 한쪽이라도 우리 DB에 없으면(과거 바쇼면 주로 은퇴자) 그 경기는 스킵하고 목록에 남긴다.
 * {@code kimarite}는 API 로마자를 그대로 저장하고(표시는 {@code KimariteDisplayUtil}), "fusen"이면
 * {@code resultType=FUZEN} + kimarite null. 결정전 판별은 API에 근거가 없어 전부 정규 대전으로 넣는다.</p>
 */
@Service
@RequiredArgsConstructor
public class TorikumiImportService {

	private static final Logger log = LoggerFactory.getLogger(TorikumiImportService.class);

	private static final int MAX_DAY = 15;
	private static final long THROTTLE_MS = 150L;

	private final SumoApiClient sumoApiClient;
	private final BashoRepository bashoRepository;
	private final TorikumiRepository torikumiRepository;
	private final RikishiRepository rikishiRepository;

	private enum Outcome { CREATED, UPDATED, SKIPPED }

	@Transactional
	public TorikumiImportResultDTO importDivision(Integer bashoId, Division division) {
		BashoEntity basho = bashoRepository.findById(bashoId)
				.orElseThrow(() -> new EntityNotFoundException("바쇼를 찾을 수 없습니다. id=" + bashoId));
		String yyyymm = basho.getExternalBashoId();
		if (yyyymm == null || yyyymm.isBlank()) {
			throw new IllegalArgumentException(
					"이 바쇼는 sumo-api에서 임포트된 바쇼가 아닙니다. 바쇼 목록에서 먼저 바쇼 임포트를 실행하세요.");
		}

		int created = 0;
		int updated = 0;
		int daysWithData = 0;
		int matchesSeen = 0;
		List<String> skipped = new ArrayList<>();

		for (int day = 1; day <= MAX_DAY; day++) {
			SumoApiTorikumiDayDTO dto;
			try {
				dto = sumoApiClient.getTorikumi(yyyymm, division, day);
			} catch (RuntimeException e) {
				skipped.add(day + "일차 조회 실패: " + e.getMessage());
				continue;
			}
			List<SumoApiTorikumiDayDTO.Match> matches = (dto == null) ? null : dto.torikumi();
			if (matches == null || matches.isEmpty()) {
				continue; // 아직 안 치러진 일차
			}
			daysWithData++;
			for (SumoApiTorikumiDayDTO.Match m : matches) {
				matchesSeen++;
				try {
					switch (upsert(basho, division, m, skipped)) {
						case CREATED -> created++;
						case UPDATED -> updated++;
						case SKIPPED -> { /* 사유는 skipped 목록에 담김 */ }
					}
				} catch (RuntimeException e) {
					skipped.add(matchLabel(m) + " 저장 실패: " + e.getMessage());
				}
			}
			throttle();
		}

		log.info("[TorikumiImport] {} {} - 신규 {}, 갱신 {}, 데이터 일차 {}, 경기 {}, 스킵 {}",
				yyyymm, division, created, updated, daysWithData, matchesSeen, skipped.size());
		return new TorikumiImportResultDTO(
				bashoLabel(basho), division, created, updated, daysWithData, matchesSeen, skipped);
	}

	private Outcome upsert(BashoEntity basho, Division division,
						   SumoApiTorikumiDayDTO.Match m, List<String> skipped) {
		if (m.id() == null || m.eastId() == null || m.westId() == null) {
			skipped.add(matchLabel(m) + " (필수 필드 없음)");
			return Outcome.SKIPPED;
		}
		RikishiEntity east = rikishiRepository.findByExternalApiId(m.eastId()).orElse(null);
		RikishiEntity west = rikishiRepository.findByExternalApiId(m.westId()).orElse(null);
		if (east == null || west == null) {
			String miss = (east == null ? "東 " + label(m.eastShikona(), m.eastId()) : "")
					+ (east == null && west == null ? " / " : "")
					+ (west == null ? "西 " + label(m.westShikona(), m.westId()) : "");
			skipped.add(matchLabel(m) + " (미매칭: " + miss + ")");
			return Outcome.SKIPPED;
		}

		RikishiEntity winner = null;
		RikishiEntity loser = null;
		Integer wid = m.winnerId();
		if (wid != null && wid.equals(m.eastId())) {
			winner = east;
			loser = west;
		} else if (wid != null && wid.equals(m.westId())) {
			winner = west;
			loser = east;
		}
		// winnerId가 0이거나 동/서 어느 쪽도 아니면 무승부/미결로 보고 승자 없이 저장

		String rawK = (m.kimarite() == null) ? null : m.kimarite().strip().toLowerCase();
		ResultType resultType = ResultType.NORMAL;
		String kimarite;
		if (rawK != null && rawK.contains("fusen")) {
			resultType = ResultType.FUZEN;
			kimarite = null;
		} else {
			kimarite = (rawK == null || rawK.isEmpty()) ? null : rawK;
		}

		TorikumiEntity existing = torikumiRepository.findByExternalId(m.id()).orElse(null);
		if (existing != null) {
			existing.updateFromApi(m.day(), division, m.matchNo(),
					east, west, winner, loser, resultType, kimarite);
			return Outcome.UPDATED;
		}
		torikumiRepository.save(TorikumiEntity.builder()
				.bashoEntity(basho)
				.day(m.day())
				.division(division)
				.matchNo(m.matchNo())
				.externalId(m.id())
				.eastRikishiEntity(east)
				.westRikishiEntity(west)
				.winnerRikishiEntity(winner)
				.loserRikishiEntity(loser)
				.resultType(resultType)
				.kimarite(kimarite)
				.isExtraMatch(false)
				.build());
		return Outcome.CREATED;
	}

	private static void throttle() {
		try {
			Thread.sleep(THROTTLE_MS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private static String matchLabel(SumoApiTorikumiDayDTO.Match m) {
		String east = m.eastShikona() != null ? m.eastShikona() : "?";
		String west = m.westShikona() != null ? m.westShikona() : "?";
		return (m.day() != null ? m.day() + "일 " : "") + east + " vs " + west;
	}

	private static String label(String shikona, Integer id) {
		return (shikona != null && !shikona.isBlank() ? shikona : "(이름 미상)") + " api id=" + id;
	}

	private static String bashoLabel(BashoEntity basho) {
		return basho.getBashoYear() + "년 " + basho.getBashoMonth().getMonthValue() + "월 "
				+ basho.getBashoMonth().getDisplayNameKr();
	}
}
