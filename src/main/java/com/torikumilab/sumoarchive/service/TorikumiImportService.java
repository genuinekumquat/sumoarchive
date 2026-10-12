package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.client.SumoApiClient;
import com.torikumilab.sumoarchive.domain.dto.TorikumiImportResultDTO;
import com.torikumilab.sumoarchive.domain.dto.sumoapi.SumoApiTorikumiDayDTO;
import com.torikumilab.sumoarchive.domain.entity.BashoEntity;
import com.torikumilab.sumoarchive.domain.entity.RikishiEntity;
import com.torikumilab.sumoarchive.domain.entity.ScheduledBoutEntity;
import com.torikumilab.sumoarchive.domain.entity.TorikumiEntity;
import com.torikumilab.sumoarchive.domain.entity.constant.Division;
import com.torikumilab.sumoarchive.domain.entity.constant.ResultType;
import com.torikumilab.sumoarchive.repository.BashoRepository;
import com.torikumilab.sumoarchive.repository.RikishiRepository;
import com.torikumilab.sumoarchive.repository.ScheduledBoutRepository;
import com.torikumilab.sumoarchive.repository.TorikumiRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * sumo-api.com에서 특정 바쇼·디비전의 15일치 대전(+ 16일째로 오는 우승 결정전)을 가져와 upsert한다.
 * 엔드포인트가 하루치({@code /torikumi/{division}/{day}})라 day 1~15를 각각 호출하고,
 * 호출 사이에 짧게 쉬어(throttle) rate limit에 대비한다. 한 일차가 실패해도 나머지는 진행한다.
 *
 * <p>매칭 키는 {@code externalId}("202405-1-1-31-25"), 리키시는 {@code externalApiId}로 찾는다.
 * 동/서 중 한쪽이라도 우리 DB에 없으면(과거 바쇼면 주로 은퇴자) 그 경기는 스킵하고 목록에 남긴다.
 * {@code kimarite}는 API 로마자를 그대로 저장하고(표시는 {@code KimariteDisplayUtil}), "fusen"이면
 * {@code resultType=FUZEN} + kimarite null. 결정전은 16일째 경기로 판별해 {@code isExtraMatch=true}.</p>
 *
 * <p>결과가 아직 없는 경기(전날 발표된 대진)는 torikumi가 아니라 scheduled_bout에 넣고, 디비전 임포트가 끝나면
 * 이번에 예정으로 보이지 않은 것(결과가 나왔거나 대진이 바뀐 것)을 지운다. 조회에 실패한 일차가 있으면
 * 그날 예정 대진을 잘못 지우지 않도록 지우기는 건너뛴다.</p>
 */
@Service
@RequiredArgsConstructor
public class TorikumiImportService {

	private static final Logger log = LoggerFactory.getLogger(TorikumiImportService.class);

	private static final int MAX_DAY = 15;
	/**
	 * sumo-api는 우승 결정전(優勝決定戦)을 16일째 경기로 준다. 결정전으로 표시(isExtraMatch)해서 받고,
	 * 전적·호시토리표·분석은 기존대로 결정전을 빼고 센다 (공식 기록도 바쇼 성적에 넣지 않음).
	 */
	private static final int PLAYOFF_DAY = 16;
	private static final long THROTTLE_MS = 150L;

	private final SumoApiClient sumoApiClient;
	private final BashoRepository bashoRepository;
	private final TorikumiRepository torikumiRepository;
	private final RikishiRepository rikishiRepository;
	private final RosterImportService rosterImportService;
	private final ScheduledBoutRepository scheduledBoutRepository;

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
		Set<String> scheduledSeen = new HashSet<>();
		boolean fetchFailed = false;

		for (int day = 1; day <= PLAYOFF_DAY; day++) {
			SumoApiTorikumiDayDTO dto;
			try {
				dto = sumoApiClient.getTorikumi(yyyymm, division, day);
			} catch (RuntimeException e) {
				skipped.add(day + "일차 조회 실패: " + e.getMessage());
				fetchFailed = true;
				continue;
			}
			List<SumoApiTorikumiDayDTO.Match> matches = (dto == null) ? null : dto.torikumi();
			if (matches == null || matches.isEmpty()) {
				continue; // 아직 안 치러진 일차
			}
			daysWithData++;
			int pending = 0;
			for (SumoApiTorikumiDayDTO.Match m : matches) {
				matchesSeen++;
				// 진행 중 바쇼는 다음 날 대진이 결과 없이(winnerId 0, kimarite "") 먼저 공개된다.
				// torikumi에 넣으면 승자 없는 경기가 화면에서 패배처럼 보이므로 예정 대진(scheduled_bout)으로 두고,
				// 결과가 나온 뒤 재임포트 때 torikumi로 넣는다.
				if (isUndecided(m)) {
					if (upsertScheduled(basho, division, m, skipped)) {
						scheduledSeen.add(m.id());
						pending++;
					}
					continue;
				}
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
			if (pending > 0) {
				skipped.add(day + "일차 예정 대진 " + pending + "경기 저장 (결과는 재임포트 시 반영)");
			}
			throttle();
		}

		if (!fetchFailed) {
			for (ScheduledBoutEntity s : scheduledBoutRepository.findByBashoEntityIdAndDivision(bashoId, division)) {
				if (!scheduledSeen.contains(s.getExternalId())) {
					scheduledBoutRepository.delete(s); // 결과가 나왔거나(이제 torikumi에 있음) 대진이 바뀜
				}
			}
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
		RikishiEntity east = resolveRikishi(m.eastId(), m.eastShikona());
		RikishiEntity west = resolveRikishi(m.westId(), m.westShikona());
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
				.isExtraMatch(m.day() != null && m.day() > MAX_DAY)
				.build());
		return Outcome.CREATED;
	}

	/** 예정 대진 저장. 필수 필드가 없거나 선수를 못 찾으면 false (목록에 사유를 남김) */
	private boolean upsertScheduled(BashoEntity basho, Division division,
									SumoApiTorikumiDayDTO.Match m, List<String> skipped) {
		if (m.id() == null || m.day() == null || m.eastId() == null || m.westId() == null) {
			skipped.add(matchLabel(m) + " (예정 대진, 필수 필드 없음)");
			return false;
		}
		RikishiEntity east = resolveRikishi(m.eastId(), m.eastShikona());
		RikishiEntity west = resolveRikishi(m.westId(), m.westShikona());
		if (east == null || west == null) {
			skipped.add(matchLabel(m) + " (예정 대진, 미매칭)");
			return false;
		}
		ScheduledBoutEntity existing = scheduledBoutRepository.findByExternalId(m.id()).orElse(null);
		if (existing != null) {
			existing.update(m.day(), m.matchNo(), east, west);
		} else {
			scheduledBoutRepository.save(ScheduledBoutEntity.builder()
					.bashoEntity(basho).day(m.day()).division(division).matchNo(m.matchNo())
					.externalId(m.id()).eastRikishiEntity(east).westRikishiEntity(west)
					.build());
		}
		return true;
	}

	/**
	 * sumo-api ID로 찾고, 없으면 단건 조회로 들여온다. 그래도 없으면 로마자 시코나가 정확히 같은 선수가 딱 한 명일 때만
	 * 그 선수로 본다 - 데뷔 바쇼 경기에만 임시 ID가 붙고 이후 새 ID로 바뀐 신인이 있어서
	 * (예: 2025년 7월 카키 9037 → 9088, 옛 ID는 조회하면 204). 동명이인이 있으면 스킵.
	 */
	private RikishiEntity resolveRikishi(Integer apiId, String shikonaEn) {
		RikishiEntity r = rikishiRepository.findByExternalApiId(apiId)
				.orElseGet(() -> rosterImportService.getOrFetchRikishi(apiId));
		if (r != null || shikonaEn == null || shikonaEn.isBlank()) {
			return r;
		}
		List<RikishiEntity> sameName = rikishiRepository.findByShikonaEn(shikonaEn.strip());
		return sameName.size() == 1 ? sameName.get(0) : null;
	}

	private static boolean isUndecided(SumoApiTorikumiDayDTO.Match m) {
		boolean noWinner = m.winnerId() == null || m.winnerId() == 0;
		boolean noKimarite = m.kimarite() == null || m.kimarite().isBlank();
		return noWinner && noKimarite;
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
