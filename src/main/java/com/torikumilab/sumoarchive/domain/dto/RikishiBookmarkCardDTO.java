package com.torikumilab.sumoarchive.domain.dto;

import java.util.List;

/**
 * 북마크 페이지(/bookmark)의 리키시 카드 한 장. LocalStorage에 저장된 ID 배열을
 * GET /api/rikishiEntity?ids=로 던지면 BookmarkPageDTO.cards로 응답한다.
 */
public record RikishiBookmarkCardDTO(
		Integer rikishiId,
		String shikonaKr,
		String shikonaJp,
		String heyaName,
		String highestRank,
		String activePeriod,
		String statusLabel,
		// 일본어 화면용 (즐겨찾기 페이지가 html lang에 맞춰 고른다)
		String heyaNameJp,
		String highestRankJp,
		String activePeriodJp,
		String statusLabelJp,
		// 이번 바쇼(BookmarkPageDTO.basho) 반즈케에 있으면 그 성적, 없으면(은퇴 등) null
		Current current
) {

	/**
	 * 이번 바쇼 성적.
	 *
	 * @param marks    경기 결과를 날짜순으로: WIN·LOSS·FUSEN_WIN·FUSEN_LOSS (화면에서 ○●□■)
	 * @param lastBout 가장 최근 경기, 아직 없으면(개막 전 등) null
	 * @param nextBout 발표된 다음 경기(예정 대진), 없으면 null
	 */
	public record Current(
			String rankKr,
			String rankJp,
			int wins,
			int losses,
			int absences,
			List<String> marks,
			LastBout lastBout,
			NextBout nextBout
	) {}

	/** 상대 계급은 그 바쇼 반즈케 기준(없으면 null) */
	public record NextBout(
			int day,
			String opponentKr,
			String opponentJp,
			String opponentRankKr,
			String opponentRankJp
	) {}

	public record LastBout(
			int day,
			boolean win,
			boolean fusen,
			String opponentKr,
			String opponentJp,
			String kimariteKr,
			String kimariteJp,
			Integer torikumiId
	) {}
}
