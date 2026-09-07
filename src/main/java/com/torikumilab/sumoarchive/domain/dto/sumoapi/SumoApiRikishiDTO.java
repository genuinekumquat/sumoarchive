package com.torikumilab.sumoarchive.domain.dto.sumoapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * sumo-api.com {@code GET /api/rikishis} / {@code /api/rikishi/{id}} 응답의 리키시 1건.
 * 필드 유무가 레코드마다 달라서(은퇴자는 shikonaJp/currentRank 등 누락) 전부 nullable로 받는다.
 * {@code intai}는 은퇴자에게만 존재하는 은퇴일(ISO). 현역 조회(intai=false)에선 항상 null.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SumoApiRikishiDTO(
		long id,
		String shikonaEn,
		String shikonaJp,
		String heya,
		String currentRank,
		String birthDate,
		String shusshin,
		Integer height,
		Integer weight,
		String debut,
		String intai
) {
}
