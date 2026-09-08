package com.torikumilab.sumoarchive.domain.dto;

import java.util.List;

/**
 * 관리자 "우승·삼상·킨보시 갱신"(바쇼 1건) 결과 요약 (화면 flash용).
 * yusho/sansho는 sumo-api {@code /api/basho/{id}}에서, kinboshi는 우리 DB의 반즈케·토리쿠미에서 파생.
 * {@code skipped}는 우리 DB에 없는 리키시 등으로 건너뛴 항목.
 */
public record AwardImportResultDTO(
		String bashoLabel,
		int yushoCreated,
		int sanshoCreated,
		int kinboshiCreated,
		List<String> skipped
) {
}
