package com.torikumilab.sumoarchive.domain.dto;

import java.util.List;

/**
 * 리키시 프로필 "상대 조건별 성적" 패널 - 상대의 계급 / 체중 차 / 키 차 구간별 승률.
 * 통산 정규 대전 기준 (결정전·부전 제외). 키·체중은 양쪽 모두 현재 값 기준.
 */
public record OpponentConditionDTO(List<Row> byRank, List<Row> byWeight, List<Row> byHeight) {

	/**
	 * @param winPercent        이 구간 승률 (%), 경기가 없으면 null
	 * @param leagueWinPercent  같은 체격 차 구간의 리그 전체 승률 (%). 계급 구간은 비교 기준이 없어 null
	 * @param smallSample       경기 수가 적어(RikishiAnalysisService.MIN_BOUTS 미만) 흐리게 표시
	 */
	public record Row(String labelKr, String labelJp, long bouts, long wins,
					  Double winPercent, Double leagueWinPercent, boolean smallSample) {
	}
}
