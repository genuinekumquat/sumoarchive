package com.torikumilab.sumoarchive.domain.dto.sumoapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * sumo-api.com {@code GET /api/basho/{YYYYMM}/banzuke/{division}} 응답.
 * 원소가 {@code east} / {@code west} 배열로 나뉘어 오고, 각 원소에도 {@code side}가 중복으로 들어있다.
 * 각 원소의 {@code record}(15일치 대전 요약)는 토리쿠미 슬라이스에서 쓰므로 여기선 무시한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SumoApiBanzukeDTO(
		String bashoId,
		String division,
		List<Entry> east,
		List<Entry> west
) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Entry(
			Integer rikishiID,
			String shikonaEn,
			String side,
			String rank,
			Integer rankValue
	) {
	}
}
