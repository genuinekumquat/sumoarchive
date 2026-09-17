package com.torikumilab.sumoarchive.domain.dto;

import java.util.List;

/**
 * 상대전적(対戦成績) 패널의 ㄱㄴㄷ 인덱스 한 그룹. initial은 초성(ㄱ~ㅎ) 또는
 * 한글이 아닌 이름이 묶이는 "#"(HangulIndexUtil.OTHER).
 */
public record HeadToHeadGroupDTO(
		String initial,
		List<HeadToHeadDTO> opponents
) {
}
