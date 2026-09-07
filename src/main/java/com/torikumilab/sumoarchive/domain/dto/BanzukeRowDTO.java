package com.torikumilab.sumoarchive.domain.dto;

import com.torikumilab.sumoarchive.domain.entity.constant.RankName;
import com.torikumilab.sumoarchive.domain.entity.constant.Side;

/** 관리자 반즈케 관리 화면의 등록 목록 한 줄. */
public record BanzukeRowDTO(
		Integer id,
		Integer rikishiId,
		String shikonaKr,
		String shikonaJp,
		RankName rankName,
		Side side,
		Integer rankValue
) {
}
