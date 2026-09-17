package com.torikumilab.sumoarchive.domain.dto;

import java.util.List;

/**
 * 리키시 상세 화면 "상대전적(対戦成績)" 패널 - 특정 상대와의 통산 전적 한 줄.
 * 집계 로직(RikishiDetailService)은 추후 구현 예정이며, 현재는 UI 틀만 우선 구성한다.
 */
public record HeadToHeadDTO(
		Integer opponentId,
		String opponentShikonaKr,
		long wins,
		long losses,
		List<HeadToHeadBoutDTO> bouts // 최신 바쇼부터 내림차순
) {
}
