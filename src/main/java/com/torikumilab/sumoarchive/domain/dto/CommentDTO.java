package com.torikumilab.sumoarchive.domain.dto;

/**
 * 토리쿠미 상세 페이지 / 슬라이드 패널의 익명 댓글 한 건.
 * 삭제된 댓글도 레이아웃 유지를 위해 목록에 남기되, 원문(content)은 내려주지 않고
 * displayContent에 안내 문구만 채운다.
 */
public record CommentDTO(
		Integer id,
		String nickName,        // 삭제된 경우 null
		String displayContent,  // 살아있으면 원문, 삭제면 안내 문구
		String createdAt,       // "2026-09-02 14:03"
		boolean deleted,
		boolean blinded         // true = 관리자 블라인드, false = (deleted면) 작성자 본인 삭제
) {
}
