package com.torikumilab.sumoarchive.domain.dto.sumoapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * sumo-api.com {@code GET /api/basho/{YYYYMM}/torikumi/{division}/{day}} 응답.
 * 하루치 대전만 준다. 아직 안 치러진 일차(예정 바쇼)는 {@code torikumi} 키 자체가 없어 null로 온다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SumoApiTorikumiDayDTO(
		String date,
		List<Match> torikumi
) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Match(
			String id,          // "202405-1-1-31-25" = bashoId-day-matchNo-eastId-westId. 매칭 키
			Integer day,
			Integer matchNo,
			Integer eastId,
			Integer westId,
			Integer winnerId,   // 0 또는 없으면 미결/무승부
			String kimarite,    // 로마자 소문자. "fusen"이면 부전(不戦)
			String eastShikona,
			String westShikona
	) {
	}
}
