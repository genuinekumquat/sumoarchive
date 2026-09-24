package com.torikumilab.sumoarchive.domain.dto;

import java.util.List;

/**
 * 키마리테 백과사전의 공식 분류별 그룹 (기본기, 던지기, 걸기 등).
 */
public record KimariteGroupDTO(
		String code,
		String categoryKr,
		String categoryJp,
		List<KimariteEntryDTO> entries
) {
}
