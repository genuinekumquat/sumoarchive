package com.torikumilab.sumoarchive.domain.dto;

import java.util.List;

/**
 * 메인 반즈케 탭 「이번 반즈케 변동」 - 직전 바쇼 반즈케와 비교한 신입막·재입막·신주료·재주료·신제자·은퇴.
 * retired는 sumo-api 은퇴일이 있는 선수, dropped는 직전 반즈케엔 있었는데 이번엔 없고 은퇴 기록도 없는 선수
 * (장기 휴장으로 반즈케 밖으로 밀려난 경우 등). 은퇴·제외의 계급은 직전 바쇼 기준.
 */
public record BanzukeChangesDTO(
		int prevYear,
		String prevMonthLabelKr,
		String prevMonthLabelJp,
		List<Entry> newMakuuchi,
		List<Entry> returnMakuuchi,
		List<Entry> newJuryo,
		List<Entry> returnJuryo,
		List<Entry> newRecruits,
		List<Entry> retired,
		List<Entry> dropped
) {

	public record Entry(
			Integer rikishiId,
			String shikonaKr,
			String shikonaJp,
			String shikonaEn,
			String rankKr,
			String rankJp,
			String heyaNameKr,
			String heyaNameJp
	) {}
}
