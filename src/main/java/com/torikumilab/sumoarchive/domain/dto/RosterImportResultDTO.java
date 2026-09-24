package com.torikumilab.sumoarchive.domain.dto;

import java.util.List;

/** 관리자 "sumo-api 로스터 임포트" 결과 요약 (화면 flash용). */
public record RosterImportResultDTO(
		int heyaCreated,
		int rikishiCreated,
		int rikishiUpdated,
		int rikishiImported,
		List<String> warnings
) {
	public RosterImportResultDTO(int heyaCreated, int rikishiImported, List<String> warnings) {
		this(heyaCreated, 0, rikishiImported, rikishiImported, warnings);
	}
}
