package com.torikumilab.sumoarchive.domain.dto;

/**
 * 관리자 리키시 목록(/admin/rikishi) 한 줄 - 수정할 대상을 찾기 위한 최소 정보만.
 * highestRank는 직접 입력값이 있으면 그 값(highestRankManual), 없으면 반즈케 계산값.
 * highestRankOutdated는 직접 입력값이 반즈케 계산값보다 낮다는 뜻 (HighestRankService.ManualCheck).
 */
public record RikishiAdminRowDTO(
		Integer id,
		String shikonaKr,
		String shikonaJp,
		boolean shikonaKrAuto,
		String heyaName,
		String highestRank,
		boolean highestRankManual,
		boolean highestRankOutdated,
		String computedHighestRank,
		String statusLabel
) {
}
