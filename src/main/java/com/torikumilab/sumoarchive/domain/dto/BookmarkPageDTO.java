package com.torikumilab.sumoarchive.domain.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * GET /api/rikishiEntity?ids= 응답: 기준 바쇼(반즈케가 있는 가장 최근 바쇼) + 요청 순서대로의 카드.
 * 기준 바쇼가 없으면(빈 DB) basho는 null.
 */
public record BookmarkPageDTO(Basho basho, List<RikishiBookmarkCardDTO> cards) {

	public enum Status { UPCOMING, IN_PROGRESS, FINISHED }

	/**
	 * @param lastDay 이 바쇼에 적재된 마지막 일차(정규 경기 기준), 아직 없으면 null
	 * @param nextDay 발표된 예정 대진 중 가장 이른 일차, 없으면 null
	 */
	public record Basho(
			String labelKr,
			String labelJp,
			String nameKr,
			String nameJp,
			Status status,
			LocalDate startDate,
			Integer lastDay,
			Integer nextDay
	) {}
}
