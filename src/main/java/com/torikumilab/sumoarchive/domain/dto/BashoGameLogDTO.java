package com.torikumilab.sumoarchive.domain.dto;

import java.util.List;

/**
 * 리키시 상세 화면 하단 "바쇼별 성적(過去の星取表)" 한 줄.
 * RikishiDetailService#getGameLog()에서 바쇼별로 하나씩 만들어줌 (최신 바쇼부터 내림차순).
 */
public record BashoGameLogDTO(
		String bashoLabel,      // "2026年07月場所" 형태
		String rankDisplay,     // 그 바쇼 기준 순위 표기 ("前頭10枚目" 등)
		String recordSummary,   // "9勝6敗" 형태 (그 바쇼에 등록된 토리쿠미 기준. 등록이 없으면 "-")
		List<MatchHistoryItemDTO> matches // day 오름차순, 가로 요약 줄(호시토리표)용
) {
}