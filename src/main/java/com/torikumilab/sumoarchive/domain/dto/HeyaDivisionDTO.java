package com.torikumilab.sumoarchive.domain.dto;

import java.util.List;

/**
 * 헤야 페이지의 계급 한 묶음. division은 메시지 키용 소문자 이름("makuuchi" → main.division.makuuchi).
 */
public record HeyaDivisionDTO(
		String division,
		List<HeyaMemberDTO> members
) {
}
