package com.torikumilab.sumoarchive.domain.dto;

import java.util.List;

/**
 * 리키시 프로필 "패배 유형" 요약 (키마리테 차트 패배 모드 아래).
 * 이 리키시가 진 경기를 LossType별로 묶어 비율을 내고, 같은 디비전 평균과 비교한 결과.
 *
 * @param totalLosses  결정기술이 있는 패배 수 (부전패 제외)
 * @param smallSample  표본이 적어(RikishiAnalysisService.MIN_LOSSES 미만) 약점/강점 판정을 하지 않음
 * @param scaleMax     막대 길이 기준값 (행들의 percent·averagePercent 중 최댓값) - 막대가 너무 짧아 보이지 않게
 */
public record LossTypeSummaryDTO(long totalLosses, boolean smallSample, double scaleMax, List<Row> rows) {

	public enum Verdict { WEAK, STRONG, NONE }

	/**
	 * @param averagePercent 이 리키시가 진 경기의 디비전 구성에 맞춘 리그 평균 비율 (%)
	 */
	public record Row(String nameKr, String nameJp, long count, double percent, double averagePercent, Verdict verdict) {
	}
}
