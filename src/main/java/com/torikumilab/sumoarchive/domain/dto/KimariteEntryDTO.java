package com.torikumilab.sumoarchive.domain.dto;

/**
 * 메인 페이지 "키마리테" 탭(결정기술 백과사전) 한 항목.
 */
public record KimariteEntryDTO(
		String kimariteKr,
		String kimariteJp,
		String descriptionKr,
		String descriptionJp
) {
	/**
	 * 기존 3인자 생성자 호환용 (descriptionKr을 기본 설명으로 사용)
	 */
	public KimariteEntryDTO(String kimariteKr, String kimariteJp, String descriptionKr) {
		this(kimariteKr, kimariteJp, descriptionKr, null);
	}

	/**
	 * 기존 호출부 호환용 getter
	 */
	public String description() {
		return descriptionKr;
	}
}
