package com.torikumilab.sumoarchive.domain.dto;

/** 관리자 헤야 관리의 일문 선택지 - 이미 어느 헤야엔가 지정된 일문(한/일명 쌍). */
public record IchimonOptionDTO(
		String ichimonKr,
		String ichimonJp
) {
}
