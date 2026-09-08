package com.torikumilab.sumoarchive.domain.dto;

/**
 * 관리자 리키시 목록(/admin/rikishi) 한 줄 - 수정할 대상을 찾기 위한 최소 정보만.
 */
public record RikishiAdminRowDTO(
		Integer id,
		String shikonaKr,
		String shikonaJp,
		boolean shikonaKrAuto,
		String heyaName,
		String highestRank,
		String statusLabel
) {
}
