package com.torikumilab.sumoarchive.domain.dto;

/**
 * 상대전적(HeadToHeadDTO) 패널을 펼쳤을 때 보이는 개별 대전 한 건.
 */
public record HeadToHeadBoutDTO(
		String bashoLabel,   // "2026年07月場所" 형태
		String bashoLabelKr, // "2026년 7월 나고야바쇼" 형태 (한국어 화면용)
		int day,
		boolean win,
		boolean fusen,       // 부전승/부전패 (실제로 붙지 않은 경기 - 천적/강세 집계에서 제외)
		String kimarite,     // 일본어 결정기술명, 부전이면 "不戦勝"/"不戦敗"
		String kimariteKr    // 한국어 결정기술명, 부전이면 "부전승"/"부전패"
) {
}
