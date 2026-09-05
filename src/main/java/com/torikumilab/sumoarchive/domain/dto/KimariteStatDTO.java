package com.torikumilab.sumoarchive.domain.dto;

/**
 * 리키시 상세 화면의 결정기술(키마리테) 원형차트용 통계 한 조각.
 * 상위 N개는 개별 키마리테로, 나머지는 "기타"로 합쳐서 RikishiDetailService에서 만들어줌.
 */
public record KimariteStatDTO(
		String kimarite, // "기타"인 경우도 포함
		long count,
		double percent   // 소수 첫째자리까지 반올림
) {
}
