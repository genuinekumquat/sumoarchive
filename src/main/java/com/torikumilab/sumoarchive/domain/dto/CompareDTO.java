package com.torikumilab.sumoarchive.domain.dto;

import java.util.List;

/**
 * 두 리키시 비교(/compare?a=&b=) 화면 전체. A·B 순서는 주소 그대로 (왼쪽 A, 오른쪽 B).
 *
 * @param fusenBouts    맞대결 중 부전 경기 수 (승수에는 포함, 결정기술 집계에서는 제외)
 * @param bouts         맞대결 목록, 최신 바쇼부터
 * @param bashoRows     진행 중인 바쇼(있으면 맨 위) + 끝난 최근 6바쇼, 최신부터
 * @param recordSinceYear 대전 기록이 시작되는 해 (반즈케가 들어간 가장 이른 바쇼). 없으면 null
 */
public record CompareDTO(
		Side a,
		Side b,
		long aWins,
		long bWins,
		long fusenBouts,
		List<Bout> bouts,
		List<KimariteCount> aKimarite,
		List<KimariteCount> bKimarite,
		List<CategoryCount> aCategories,
		List<CategoryCount> bCategories,
		List<BashoRow> bashoRows,
		Integer recordSinceYear
) {

	public boolean hasBouts() {
		return !bouts.isEmpty();
	}

	/** 선수 카드. rank*는 가장 최근 반즈케 기준 ("동 오제키1"), 반즈케가 없으면 null. */
	public record Side(
			Integer id,
			String shikonaKr,
			String shikonaJp,
			String heyaNameKr,
			String heyaNameJp,
			String rankKr,
			String rankJp,
			boolean active
	) {}

	public record Bout(
			Integer torikumiId,
			String bashoLabelKr,
			String bashoLabelJp,
			int day,
			boolean aWon,
			boolean fusen,
			String kimariteKr,
			String kimariteJp
	) {}

	public record KimariteCount(String kr, String jp, long count) {}

	/** percent는 그 선수가 맞대결에서 이긴 경기(부전 제외) 중 비율. */
	public record CategoryCount(String kr, String jp, long count, double percent) {}

	public record BashoRow(String labelKr, String labelJp, boolean inProgress, Cell a, Cell b) {}

	/**
	 * 한 바쇼의 한 선수 칸. 그 바쇼 반즈케에 없으면 칸 자체가 null (화면에 "-").
	 * 반즈케에는 있는데 아직 경기가 없으면 record*가 "-".
	 */
	public record Cell(String rankKr, String rankJp, String recordKr, String recordJp,
					   boolean kachikoshi, boolean makekoshi) {}
}
