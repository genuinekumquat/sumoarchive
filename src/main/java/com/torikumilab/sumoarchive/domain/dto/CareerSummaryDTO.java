package com.torikumilab.sumoarchive.domain.dto;

/**
 * "바쇼별 성적" 패널 맨 위 커리어 요약 - 기록된 바쇼 기준 카치코시 수, 두 자릿수 승리, 최고 성적, 휴장.
 * 끝난 바쇼를 전 디비전에서 센다 (진행 중 바쇼는 중간 성적이라 뺀다).
 *
 * @param basho       집계 대상 바쇼 수 (카치코시 비율의 분모. 全休도 포함)
 * @param kachikoshi  치를 경기의 과반을 이긴 바쇼 수 (세키토리 8승, 마쿠시타 이하 4승)
 * @param doubleDigit 세키토리 바쇼 중 10승 이상
 * @param absence     휴장이 하루라도 있는 바쇼 수 (全休 포함)
 * @param zenkyu      全休 바쇼 수
 * @param best        세키토리 바쇼 중 승수가 가장 많은 바쇼 (같으면 최근 바쇼). 없으면 null
 */
public record CareerSummaryDTO(int basho, int kachikoshi, int doubleDigit, int absence, int zenkyu,
							   BashoGameLogDTO best) {

	public boolean isEmpty() {
		return basho == 0;
	}

	/** 카치코시 비율 (%, 반올림 정수) */
	public int kachikoshiPercent() {
		return basho == 0 ? 0 : Math.round(kachikoshi * 100f / basho);
	}
}
