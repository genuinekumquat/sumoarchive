package com.torikumilab.sumoarchive.domain.dto;

import java.util.List;

/**
 * 키마리테 백과사전의 ㄱㄴㄷ 초성 인덱스 한 그룹. rikishi/detail.html 상대전적 패널에서 쓴
 * HeadToHeadGroupDTO/HangulIndexUtil 패턴과 동일.
 */
public record KimariteGroupDTO(
		String initial,
		List<KimariteEntryDTO> entries
) {
}
