package com.torikumilab.sumoarchive.domain.dto;

import java.util.List;

/**
 * 리키시 상세 화면 "상대전적(対戦成績)" 패널 - 특정 상대와의 통산 전적 한 줄.
 */
public record HeadToHeadDTO(
		Integer opponentId,
		String opponentShikonaKr,
		String opponentShikonaJp, // 일본어 화면용
		String opponentShikonaEn, // 로마자 시코나 - 일본어 화면 あかさたな 탭 행·정렬용 (KanaIndexUtil)
		long wins,
		long losses,
		List<HeadToHeadBoutDTO> bouts, // 최신 바쇼부터 내림차순
		boolean opponentActive    // 상대가 현역인지 (현역 선수의 천적/강한 상대 요약은 현역 상대만)
) {
}
