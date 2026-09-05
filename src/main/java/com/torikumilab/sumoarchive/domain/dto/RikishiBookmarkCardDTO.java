package com.torikumilab.sumoarchive.domain.dto;

/**
 * 북마크 페이지(/bookmark)의 리키시 카드 한 장. LocalStorage에 저장된 ID 배열을
 * GET /api/rikishiEntity?ids=로 던지면 이 DTO 목록으로 응답한다.
 */
public record RikishiBookmarkCardDTO(
		Integer rikishiId,
		String shikonaKr,
		String shikonaJp,
		String heyaName,
		String highestRank,
		String activePeriod,
		String statusLabel,
		String photoUrl
) {
}
