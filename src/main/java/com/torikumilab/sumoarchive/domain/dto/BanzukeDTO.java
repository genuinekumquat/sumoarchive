package com.torikumilab.sumoarchive.domain.dto;

public record BanzukeDTO(
		Integer rikishiId,
		String shikonaKr,
		String rankName,   // String으로 변환
		String side,
		Integer rankValue,
		boolean isActive
) {}