package com.torikumilab.sumoarchive.domain.dto;

import java.time.LocalDate;

/** 관리자 바쇼 목록(/admin/basho) 한 줄. entryCount는 디비전 무관 전체 반즈케 등록 수. */
public record BashoAdminRowDTO(
		Integer id,
		int year,
		String monthLabelKr,
		int month,
		LocalDate startDate,
		LocalDate endDate,
		long entryCount
) {
}
