package com.torikumilab.sumoarchive.domain.dto;

import java.util.List;

/**
 * 리키시 상세 화면 하단 "바쇼별 성적(過去の星取表)" 한 줄.
 * RikishiDetailService#getGameLog()에서 바쇼별로 하나씩 만들어줌 (최신 바쇼부터 내림차순).
 */
public record BashoGameLogDTO(
		Integer bashoId,        // 결정기술 차트 기간 필터(select) 옵션값으로도 씀
		String bashoLabel,      // "2026年07月場所" 형태
		String bashoLabelKr,    // "2026년 7월 나고야바쇼" 형태 (한국어 화면용)
		String rankDisplay,     // 그 바쇼 기준 동서+순위 표기 ("東 前頭10枚目" 등)
		String rankDisplayKr,   // "동 마에가시라10" 등
		String recordSummary,   // "9勝6敗" 형태 (그 바쇼에 등록된 토리쿠미 기준. 全休면 "0勝0敗15休", 등록이 없으면 "-")
		String recordSummaryKr, // "9승 6패" 형태 (휴장 있으면 "9승 3패 3휴")
		List<MatchHistoryItemDTO> matches // day 오름차순, 가로 요약 줄(호시토리표)용. 全休면 전 일차가 휴장(ABSENT)
) {
}