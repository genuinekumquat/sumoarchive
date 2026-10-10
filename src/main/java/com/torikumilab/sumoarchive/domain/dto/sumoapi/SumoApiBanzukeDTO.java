package com.torikumilab.sumoarchive.domain.dto.sumoapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * sumo-api.com {@code GET /api/basho/{YYYYMM}/banzuke/{division}} 응답.
 * 원소가 {@code east} / {@code west} 배열로 나뉘어 오고, 각 원소에도 {@code side}가 중복으로 들어있다.
 * 각 원소의 {@code record}(15일치 대전 요약)는 무시하고, 바쇼 성적은 {@code wins/losses/absences}를 쓴다.
 * (record는 마쿠시타 이하에서 경기 없는 날도 "absent"로 채워져 와서 휴장과 구분되지 않는다)
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
			Integer rankValue,
			Integer wins,
			Integer losses,
			Integer absences
	) {
	}
}
