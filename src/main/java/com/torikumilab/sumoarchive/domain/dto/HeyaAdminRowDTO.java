package com.torikumilab.sumoarchive.domain.dto;

/**
 * 관리자 헤야 관리 화면의 한 줄. memberCount = 최신 반즈케 기준 소속 인원 (0이면 폐업·개명 전 이름일 수 있음).
 * mergedIntoId/NameEn = 관리자가 다른 헤야로 합친(개명·흡수) 경우 그 헤야.
 */
public record HeyaAdminRowDTO(
		Integer id,
		String nameKr,
		String nameJp,
		String nameEn,
		boolean nameKrAuto,
		String ichimonKr,
		long memberCount,
		Integer mergedIntoId,
		String mergedIntoNameEn
) {
	public boolean merged() {
		return mergedIntoId != null;
	}

	/** 명단 임포트가 새로 만든 헤야는 일본어명 자리에 로마자가 그대로 들어가 있다. */
	public boolean nameJpPlaceholder() {
		return nameJp == null || nameJp.equalsIgnoreCase(nameEn) || nameJp.matches("[A-Za-z\\s-]+");
	}

	/** 지금 소속 선수가 있는데 일문 미지정(메인 일문 탭에 안 나옴)이거나 일본어명이 로마자 그대로인 헤야 - 새로 생겼거나 이름이 바뀐 헤야. */
	public boolean needsAttention() {
		return !merged() && memberCount > 0 && (ichimonKr == null || nameJpPlaceholder());
	}
}
