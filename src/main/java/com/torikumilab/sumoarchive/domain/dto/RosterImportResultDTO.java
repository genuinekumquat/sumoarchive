package com.torikumilab.sumoarchive.domain.dto;

import java.util.List;

/** 관리자 "sumo-api 로스터 임포트" 결과 요약 (화면 flash용). */
public record RosterImportResultDTO(
		int heyaCreated,
		int rikishiCreated,
		int rikishiUpdated,
		int rikishiImported,
		List<String> warnings,
		List<ShikonaChange> shikonaChanges // 이번 동기화에서 감지한 개명 - 새 한글 시코나는 자동 음차값이라 검수 대상
) {
	public RosterImportResultDTO(int heyaCreated, int rikishiCreated, int rikishiUpdated, int rikishiImported, List<String> warnings) {
		this(heyaCreated, rikishiCreated, rikishiUpdated, rikishiImported, warnings, List.of());
	}

	public RosterImportResultDTO(int heyaCreated, int rikishiImported, List<String> warnings) {
		this(heyaCreated, 0, rikishiImported, rikishiImported, warnings);
	}

	/** 개명 1건. rikishiId는 우리 DB id (관리자 수정 화면 링크용). */
	public record ShikonaChange(Integer rikishiId, String oldShikonaKr, String newShikonaJp, String newShikonaKr) {
	}
}
