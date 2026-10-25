package com.torikumilab.sumoarchive.domain.dto.sumoapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * sumo-api.com {@code GET /api/ranks?rikishiId=} 응답의 바쇼 1건 (예: bashoId "202611", rank "Yokozuna 1 East").
 * 반즈케 밖의 기록도 섞여 온다 - rank가 "Mae-zumo"(마에즈모), "Banzuke-gai"(반즈케 외).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SumoApiRankDTO(
		String bashoId,
		String rank
) {
}
