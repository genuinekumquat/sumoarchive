package com.torikumilab.sumoarchive.domain.dto.sumoapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * sumo-api.com {@code GET /api/shikonas?rikishiId=} 응답 1건 - 시코나 표기가 바뀐 바쇼마다 한 줄
 * (예: bashoId "202611", shikonaEn "Ikawasakura", shikonaJp "井川桜良(いかわさくら)").
 * 입문 초기 기록은 shikonaJp가 빈 문자열이고 shikonaEn에 뒷이름이 붙어 오기도 한다 ("Kanpuzan Ryota").
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SumoApiShikonaDTO(
		String bashoId,
		String shikonaEn,
		String shikonaJp
) {
}
