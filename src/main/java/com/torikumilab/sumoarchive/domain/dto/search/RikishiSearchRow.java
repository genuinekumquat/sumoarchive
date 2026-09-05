package com.torikumilab.sumoarchive.domain.dto.search;

import com.torikumilab.sumoarchive.domain.entity.constant.MatchType;

// DTO가 아니라 그냥 raw 값 객체라 이름 그대로 유지

public record RikishiSearchRow(
		Integer rikishiId,
		String name,
		String shikonaKr,
		String shikonaJp,
		String heyaName,
		String highestRank,
		Integer debutYear,
		Integer retirementYear,
		Boolean isActive,
		String oyakataNameKr,
		MatchType matchType
) {}

// DB raw 값용 DTO