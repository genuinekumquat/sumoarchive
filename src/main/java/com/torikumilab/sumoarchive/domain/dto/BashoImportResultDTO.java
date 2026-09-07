package com.torikumilab.sumoarchive.domain.dto;

import java.util.List;

/** 관리자 "sumo-api 바쇼 임포트" 결과 요약 (화면 flash용). */
public record BashoImportResultDTO(
		int fromYear,
		int toYear,
		int created,
		int updated,
		List<String> skipped
) {
}
