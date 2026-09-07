package com.torikumilab.sumoarchive.domain.dto.sumoapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** sumo-api.com {@code GET /api/rikishis} 페이지 응답. {@code records}는 결과가 없으면 null로 온다. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SumoApiRikishiPageDTO(
		int limit,
		int skip,
		int total,
		List<SumoApiRikishiDTO> records
) {
}
