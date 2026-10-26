package com.torikumilab.sumoarchive.domain.dto;

/**
 * 프로필 수상 기록 칸을 펼쳤을 때의 한 줄. 우승·삼상은 바쇼만, 킨보시는 일차·상대 요코즈나·대전 링크까지.
 */
public record AwardHistoryItemDTO(
		String bashoLabel,           // "2023年11月場所"
		String bashoLabelKr,         // "2023년 11월 큐슈바쇼"
		Integer day,                 // 킨보시만: 일차
		Integer torikumiId,          // 킨보시만: 대전 상세 링크
		Integer opponentId,          // 킨보시만: 진 요코즈나 id
		String opponentShikonaJp,
		String opponentShikonaKr
) {
	public boolean isKinboshi() {
		return torikumiId != null;
	}
}
