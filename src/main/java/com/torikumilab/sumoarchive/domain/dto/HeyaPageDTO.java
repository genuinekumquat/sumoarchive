package com.torikumilab.sumoarchive.domain.dto;

import java.util.List;

/**
 * 헤야 페이지(/heya/{id}) - 헤야 정보와, 반즈케가 있는 최신 바쇼 기준 소속 선수를 계급별로.
 * 반즈케가 하나도 없으면 basho 라벨은 null, divisions는 빈 목록.
 */
public record HeyaPageDTO(
		Integer heyaId,
		String nameKr,
		String nameJp,
		String ichimonKr,
		String ichimonJp,
		Integer bashoYear,
		String bashoNameKr, // "아키바쇼"
		String bashoNameJp, // "秋場所"
		List<HeyaDivisionDTO> divisions
) {
	public int memberCount() {
		return divisions.stream().mapToInt(d -> d.members().size()).sum();
	}
}
