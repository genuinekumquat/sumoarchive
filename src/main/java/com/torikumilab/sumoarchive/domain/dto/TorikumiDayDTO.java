package com.torikumilab.sumoarchive.domain.dto;

import java.util.List;

/**
 * 메인 "대진" 탭: 한 바쇼·디비전의 하루치 대진. GET /api/torikumi/day 응답.
 *
 * @param day  보여주는 일차 (16 = 우승 결정전). 이 바쇼·디비전에 경기가 하나도 없으면 null
 * @param days 고를 수 있는 일차와 상태 (일차 오름차순)
 */
public record TorikumiDayDTO(
		Integer bashoId,
		String bashoLabelKr,
		String bashoLabelJp,
		Integer day,
		List<DayStatus> days,
		List<Bout> bouts
) {

	public enum Status { DONE, SCHEDULED }

	/** DONE = 결과가 있는 날, SCHEDULED = 대진만 발표된 날 */
	public record DayStatus(int day, Status status) {}

	/**
	 * 경기 하나. 예정 경기는 winner·kimarite·torikumiId가 null.
	 *
	 * @param winner "EAST" / "WEST", 예정이거나 승부 없음이면 null
	 */
	public record Bout(
			Side east,
			Side west,
			boolean scheduled,
			String winner,
			boolean fusen,
			String kimariteKr,
			String kimariteJp,
			Integer torikumiId
	) {}

	/** 한쪽 선수. 성적(wins/losses)은 이 경기 전날까지의 정규 경기 기준 */
	public record Side(
			Integer rikishiId,
			String nameKr,
			String nameJp,
			String rankKr,
			String rankJp,
			int wins,
			int losses
	) {}
}
