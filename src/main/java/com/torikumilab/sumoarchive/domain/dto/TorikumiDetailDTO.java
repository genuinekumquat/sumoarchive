package com.torikumilab.sumoarchive.domain.dto;

/**
 * 토리쿠미(경기) 상세. 전체 페이지(/torikumi/{id})와 리키시 프로필의 슬라이드 패널
 * (/torikumi/{id}/fragment)이 같은 DTO를 공유한다. RikishiDetailService와 톤을 맞춰
 * 화면 표기용 문자열은 서버에서 최대한 만들어서 내려준다.
 */
public record TorikumiDetailDTO(
		Integer id,

		// --- 대회 정보 ---
		String bashoTitleKr,   // "2026년 7월 나고야바쇼"
		String bashoLabelJp,   // "2026年07月場所"
		Integer day,
		String dayLabelKr,     // "3일째"
		String dayLabelJp,     // "3日目"
		String divisionLabel,  // "마쿠우치" 등

		// --- 동(東) ---
		Integer eastRikishiId,
		String eastShikonaKr,
		String eastShikonaJp,
		String eastRankDisplay, // 그 바쇼 기준. 없으면 null
		boolean eastWon,

		// --- 서(西) ---
		Integer westRikishiId,
		String westShikonaKr,
		String westShikonaJp,
		String westRankDisplay,
		boolean westWon,

		// --- 결과 ---
		boolean decided,        // 승자 정보가 있는지 (무승부/미입력 방지)
		boolean fusen,          // 부전승/부전패 여부
		String kimariteKr,      // "요리키리" (부전이면 null)
		String kimariteJp,      // "寄り切り"

		// --- 미디어 / 해설 ---
		String youtubeEmbedUrl, // 임베드 가능하면 채워짐
		String youtubeUrl,      // 원본 (임베드 불가 시 링크로)
		String descriptionKr,
		String descriptionJp
) {
}
