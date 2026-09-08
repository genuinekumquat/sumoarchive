package com.torikumilab.sumoarchive.domain.dto.sumoapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * sumo-api.com {@code GET /api/basho/{YYYYMM}} 응답.
 * 미개최(예정) 바쇼는 {@code date/startDate/endDate}만 오고 location/yusho/specialPrizes는 없다(null).
 *
 * <p>{@code yusho[].type}은 디비전명("Makuuchi"..)이라 {@code Division} enum과 철자가 일치한다.
 * {@code specialPrizes[].type}은 "Shukun-sho" / "Kanto-sho" / "Gino-sho"(삼상, 마쿠우치 전용)이며
 * 같은 바쇼에 같은 상을 두 명이 받을 수도 있다(rikishiId로 구분).</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SumoApiBashoDTO(
		String date,
		String location,
		String startDate,
		String endDate,
		List<Honor> yusho,
		List<Honor> specialPrizes
) {

	/** yusho / specialPrizes 원소 공통 형태. */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Honor(
			String type,
			Integer rikishiId,
			String shikonaEn,
			String shikonaJp
	) {
	}
}
