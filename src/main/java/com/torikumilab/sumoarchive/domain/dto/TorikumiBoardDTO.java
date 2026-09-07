package com.torikumilab.sumoarchive.domain.dto;

import com.torikumilab.sumoarchive.domain.entity.constant.Division;

import java.util.List;

/** 관리자 토리쿠미 관리 화면(/admin/basho/{id}/torikumi) 한 판: 바쇼 메타 + 그 날/디비전 경기 목록. */
public record TorikumiBoardDTO(
		Integer bashoId,
		String bashoLabelKr,
		int day,
		Division division,
		List<TorikumiRowDTO> rows
) {
}
