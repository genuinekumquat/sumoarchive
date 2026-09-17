package com.torikumilab.sumoarchive.domain.dto;

import java.util.List;

/**
 * 메인 페이지 "일문" 탭 - 일문(一門) 한 그룹. 소속 헤야 수 내림차순으로 반환됨
 * (BanzukeService#getIchimonStructure).
 */
public record IchimonGroupDTO(
		String ichimonKr,
		String ichimonJp,
		List<HeyaSekitoriDTO> heyaList
) {
}
