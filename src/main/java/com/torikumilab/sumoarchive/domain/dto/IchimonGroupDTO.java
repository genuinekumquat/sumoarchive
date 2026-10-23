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
	// 접힌 일문 제목 옆에 "헤야 N · 세키토리 M"으로 표시
	public int sekitoriCount() {
		return heyaList.stream().mapToInt(h -> h.sekitori().size()).sum();
	}
}
