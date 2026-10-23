package com.torikumilab.sumoarchive.domain.dto;

/**
 * 헤야 페이지(/heya/{id})의 소속 선수 한 명 - 최신 반즈케 기준 지위와 출신지.
 */
public record HeyaMemberDTO(
		Integer rikishiId,
		String shikonaKr,
		String shikonaJp,
		String rankDisplayKr, // "마에가시라10" 등 (RankDisplayUtil.rankDisplayKorean)
		String rankDisplayJp, // "前頭10枚目" 등 (RankDisplayUtil.rankDisplay)
		String originKr,
		String originJp
) {
}
