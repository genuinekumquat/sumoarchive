package com.torikumilab.sumoarchive.domain.dto;

import com.torikumilab.sumoarchive.domain.entity.constant.Division;
import com.torikumilab.sumoarchive.domain.entity.constant.ResultType;
import com.torikumilab.sumoarchive.domain.entity.constant.Side;

/** 관리자 토리쿠미 관리 화면의 한 경기 목록 행. winnerSide가 null이면 승부 미정. */
public record TorikumiRowDTO(
		Integer id,
		int day,
		Division division,
		Integer eastId,
		String eastShikona,
		Integer westId,
		String westShikona,
		Side winnerSide,
		ResultType resultType,
		String kimarite,
		boolean extraMatch,
		boolean hasYoutube
) {
}
