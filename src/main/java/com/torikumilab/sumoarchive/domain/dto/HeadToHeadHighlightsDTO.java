package com.torikumilab.sumoarchive.domain.dto;

import java.util.List;

/**
 * 상대전적 패널 맨 위 요약 - "유독 약한 상대(천적)"와 "유독 강한 상대" 각 최대 3명.
 * 두 선수의 평소(통산) 승률로 예상한 승수보다 실제로 크게 지거나 이긴 상대만, 차이가 큰 순.
 * 부전을 뺀 실제 맞대결이 minBouts번 이상이고 예상과의 차이가 minDiff승 이상이어야 한다.
 *
 * @param minBouts / minDiff 화면 안내 문구에 기준을 그대로 보여주기 위해 같이 내려준다.
 */
public record HeadToHeadHighlightsDTO(int minBouts, double minDiff, List<Row> tough, List<Row> favorable) {

	public boolean isEmpty() {
		return tough.isEmpty() && favorable.isEmpty();
	}

	/**
	 * @param wins / losses 부전 제외 맞대결 승패
	 * @param diff          실제 승수 - 예상 승수 (소수 첫째 자리). 천적은 음수, 강한 상대는 양수
	 */
	public record Row(Integer opponentId, String opponentShikonaKr, String opponentShikonaJp,
					  long wins, long losses, double diff) {

		/** 화면용 절댓값 ("예상보다 1.8승 적음") */
		public double absDiff() {
			return Math.abs(diff);
		}
	}
}
