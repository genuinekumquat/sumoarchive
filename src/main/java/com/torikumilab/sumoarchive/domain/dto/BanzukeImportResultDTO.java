package com.torikumilab.sumoarchive.domain.dto;

import com.torikumilab.sumoarchive.domain.entity.constant.Division;

import java.util.List;

/** 관리자 "sumo-api 반즈케 임포트"(바쇼 1개 · 디비전 1개) 결과 요약 (화면 flash용). */
public record BanzukeImportResultDTO(
		String bashoLabelKr,
		Division division,
		int created,
		int updated,
		List<String> unmatched
) {
}
