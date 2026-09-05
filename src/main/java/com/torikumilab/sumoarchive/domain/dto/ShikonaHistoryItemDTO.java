package com.torikumilab.sumoarchive.domain.dto;

import java.time.LocalDate;

/**
 * 리키시 상세 화면의 시코나 변경 이력 한 줄.
 */
public record ShikonaHistoryItemDTO(
		String shikonaKr,
		LocalDate validFrom,
		LocalDate validTo // null이면 해당 이력 이후 계속 사용 (보통 마지막 과거 이력만 이 케이스)
) {
}
