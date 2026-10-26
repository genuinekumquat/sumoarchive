package com.torikumilab.sumoarchive.domain.dto;

import java.util.List;

/**
 * 프로필 수상 기록 한 칸 (마쿠우치 우승, 수훈상, 킨보시 등). 횟수는 items 개수이고,
 * 칸을 펼치면 items(오래된 바쇼부터)가 보인다. 기록이 없어도 0회 칸으로 내려간다.
 */
public record AwardGroupDTO(
		String labelKey,                 // 상 이름 메시지 키 (rikishi.award.makuuchi 등)
		boolean kinboshi,                // 단위가 個/개 (나머지는 回/회)
		List<AwardHistoryItemDTO> items
) {
	public int count() {
		return items.size();
	}
}
