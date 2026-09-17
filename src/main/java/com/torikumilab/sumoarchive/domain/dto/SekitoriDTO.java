package com.torikumilab.sumoarchive.domain.dto;

/**
 * 일문별 헤야-세키토리 구조(메인 페이지 "일문" 탭) 한 명. 세키토리 = 마쿠우치+주료 소속 선수.
 */
public record SekitoriDTO(
		Integer rikishiId,
		String shikonaKr,
		String shikonaJp,
		String rankDisplayKr, // "마에가시라10" 등 (RankDisplayUtil.rankDisplayKorean)
		String rankDisplayJp  // "前頭10枚目" 등 (RankDisplayUtil.rankDisplay)
) {
}
