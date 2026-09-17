package com.torikumilab.sumoarchive.domain.dto;

/**
 * 상대전적(HeadToHeadDTO) 패널을 펼쳤을 때 보이는 개별 대전 한 건.
 */
public record HeadToHeadBoutDTO(
		String bashoLabel, // "2026年07月場所" 형태
		int day,
		boolean win,
		String kimarite
) {
}
