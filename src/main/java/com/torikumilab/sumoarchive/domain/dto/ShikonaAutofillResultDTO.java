package com.torikumilab.sumoarchive.domain.dto;

import java.util.List;

/**
 * 관리자 "한국어 시코나 자동 채우기"(로마자 음차) 결과 요약 (화면 flash용).
 * {@code samples}는 확인용 미리보기("Asanoyama → 아사노야마") 몇 건, {@code failed}는 변환 실패한 로마자.
 */
public record ShikonaAutofillResultDTO(
		int filled,
		List<String> samples,
		List<String> failed
) {
}
