package com.torikumilab.sumoarchive.domain.dto;

import com.torikumilab.sumoarchive.domain.entity.constant.Division;

import java.util.List;

/** 관리자 "sumo-api 토리쿠미 임포트"(바쇼 1개 · 디비전 1개 · 최대 15일) 결과 요약 (화면 flash용). */
public record TorikumiImportResultDTO(
		String bashoLabelKr,
		Division division,
		int created,
		int updated,
		int daysWithData,
		int matchesSeen,
		List<String> skipped
) {
}
