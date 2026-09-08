package com.torikumilab.sumoarchive.domain.dto;

/** 관리자 헤야 관리 화면의 한 줄. */
public record HeyaAdminRowDTO(
		Integer id,
		String nameKr,
		String nameJp,
		String nameEn,
		boolean nameKrAuto
) {
}
