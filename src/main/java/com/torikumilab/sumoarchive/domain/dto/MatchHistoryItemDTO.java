package com.torikumilab.sumoarchive.domain.dto;

import com.torikumilab.sumoarchive.domain.entity.constant.Side;

/**
 * 리키시 상세 화면 호시토리표(星取表) 한 줄. RikishiDetailService#getGameLog()가 바쇼별로
 * day 1~15 오름차순으로 만들어줌 (등록된 토리쿠미가 없는 날은 status=ABSENT로 채워서 반환).
 */
public record MatchHistoryItemDTO(
		Integer day,
		Integer torikumiId,          // 이 경기의 토리쿠미 id (상세/슬라이드 패널 링크용). 휴장(ABSENT)이면 null
		Integer opponentId,          // 상대 리키시 id (상세페이지 링크용). 휴장(ABSENT)이면 null
		String opponentShikonaKr,    // 휴장이면 null
		String opponentRankDisplay,  // 그 바쇼 기준 상대 반즈케 표기. 반즈케 정보 없거나 휴장이면 null
		Side mySide,                 // 이 경기에서 조회 대상 리키시가 동/서 어느 쪽이었는지. 휴장이면 null
		Status status,
		String resultDisplay         // 결정기술명, 부전승/부전패면 "不戦勝"/"不戦敗", 휴장이면 "休場"
) {
	public enum Status { WIN, LOSS, ABSENT }
}