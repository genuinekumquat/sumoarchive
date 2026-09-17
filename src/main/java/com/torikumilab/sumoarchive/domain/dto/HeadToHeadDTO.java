package com.torikumilab.sumoarchive.domain.dto;

import java.util.List;

/**
 * 리키시 상세 화면 "상대전적(対戦成績)" 패널 - 특정 상대와의 통산 전적 한 줄.
 */
public record HeadToHeadDTO(
		Integer opponentId,
		String opponentShikonaKr,
		String opponentShikonaJp, // 일본어 화면용
		long wins,
		long losses,
		List<HeadToHeadBoutDTO> bouts // 최신 바쇼부터 내림차순
) {
}
