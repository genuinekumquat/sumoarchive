package com.torikumilab.sumoarchive.domain.dto;

/**
 * 메인 페이지 "키마리테" 탭(결정기술 백과사전) 한 항목.
 * description은 아직 콘텐츠가 없어서 항상 null - UI 틀만 먼저 만들고 나중에 채워 넣는다.
 */
public record KimariteEntryDTO(
		String kimariteKr,
		String kimariteJp,
		String description // TODO: 관리자가 채워 넣기 전까지는 항상 null
) {
}
