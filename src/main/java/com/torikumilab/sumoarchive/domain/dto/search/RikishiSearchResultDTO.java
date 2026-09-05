package com.torikumilab.sumoarchive.domain.dto.search;

public record RikishiSearchResultDTO(
		Integer rikishiId,
		String name,
		String shikonaKr,
		String shikonaJp,
		String heyaName,
		String highestRank,
		String activePeriod,
		String statusLabel,
		String oyakataNameKr
) {}

// 화면 표시용 DTO