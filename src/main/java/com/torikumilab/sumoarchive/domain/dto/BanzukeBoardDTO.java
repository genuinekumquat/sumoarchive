package com.torikumilab.sumoarchive.domain.dto;

import com.torikumilab.sumoarchive.domain.entity.constant.Division;

import java.util.List;

/** 관리자 반즈케 관리 화면(/admin/basho/{id}/banzuke) 한 판: 바쇼 메타 + 해당 디비전 등록 목록. */
public record BanzukeBoardDTO(
		Integer bashoId,
		String bashoLabelKr,
		Division division,
		List<BanzukeRowDTO> rows
) {
}
