package com.torikumilab.sumoarchive.domain.dto;

/**
 * "바쇼별 성적" 패널 맨 위 커리어 요약 - 기록된 바쇼 기준 가치코시 수, 두 자릿수 승리, 최고 성적, 휴장.
 * 끝난 세키토리(마쿠우치·쥬료) 바쇼만 센다. 진행 중 바쇼는 중간 성적이라 빼고,
 * 마쿠시타 이하는 대전 데이터가 없어서 뺀다 (15일 기준 8승 가치코시 판정이 맞지 않기도 함).
 *
 * @param basho       집계 대상 바쇼 수 (가치코시 비율의 분모. 全休도 포함)
 * @param kachikoshi  8승 이상 바쇼 수
 * @param doubleDigit 10승 이상 바쇼 수
 * @param absence     휴장이 하루라도 있는 바쇼 수 (全休 포함)
 * @param zenkyu      全休 바쇼 수
 * @param best        승수가 가장 많은 바쇼 (같으면 최근 바쇼). 모두 全休면 null
 */
public record CareerSummaryDTO(int basho, int kachikoshi, int doubleDigit, int absence, int zenkyu,
							   BashoGameLogDTO best) {

	public boolean isEmpty() {
		return basho == 0;
	}

	/** 가치코시 비율 (%, 반올림 정수) */
	public int kachikoshiPercent() {
		return basho == 0 ? 0 : Math.round(kachikoshi * 100f / basho);
	}
}
