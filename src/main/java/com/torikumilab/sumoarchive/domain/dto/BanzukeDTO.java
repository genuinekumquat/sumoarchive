package com.torikumilab.sumoarchive.domain.dto;

public record BanzukeDTO(
		Integer rikishiId,
		String shikonaKr,
		String shikonaJp,  // shikonaKr 폴백용 (sumo-api 임포트 직후 한국어 시코나는 아직 비어 있음)
		String shikonaEn,  // shikonaKr·shikonaJp 모두 없을 때 최종 폴백
		String rankName,   // String으로 변환
		String side,
		Integer rankValue,
		boolean isActive
) {}