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
		String oyakataNameKr,
		// 일본어 화면용 (검색 페이지가 언어에 맞춰 고른다)
		String heyaNameJp,
		String highestRankJp,
		String activePeriodJp,
		String oyakataNameJp
) {}

// 화면 표시용 DTO