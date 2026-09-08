package com.torikumilab.sumoarchive.domain.dto;

import java.util.List;

/**
 * 관리자 "한국어 헤야명 자동 채우기"(로마자 음차) 결과 요약 (화면 flash용).
 * {@code samples}는 확인용 미리보기("Takasago → 타카사고") 몇 건, {@code failed}는 변환 실패한 로마자.
 */
public record HeyaNameAutofillResultDTO(
		int filled,
		List<String> samples,
		List<String> failed
) {
}
