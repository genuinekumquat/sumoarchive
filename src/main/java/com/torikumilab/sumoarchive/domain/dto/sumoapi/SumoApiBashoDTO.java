package com.torikumilab.sumoarchive.domain.dto.sumoapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * sumo-api.com {@code GET /api/basho/{YYYYMM}} 응답.
 * 미개최(예정) 바쇼는 {@code date/startDate/endDate}만 오고 location/yusho/specialPrizes는 없다.
 * 슬라이스 2에선 코어 필드만 쓰고 우승/삼상(yusho, specialPrizes)은 후속 작업으로 미룬다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SumoApiBashoDTO(
		String date,
		String location,
		String startDate,
		String endDate
) {
}
