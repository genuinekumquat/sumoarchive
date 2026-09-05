package com.torikumilab.sumoarchive.domain.dto;

/**
 * 관리자 댓글 관리 대시보드(/admin/comments) 한 줄. 어느 경기의 댓글인지 알 수 있도록
 * matchLabel/torikumiId를 함께 내려준다 (CommentDTO는 이미 그 경기 페이지 안이라 불필요).
 */
public record AdminCommentDTO(
		Integer id,
		Integer torikumiId,
		String matchLabel,      // "2026년 7월 나고야바쇼 3일째 · 아사노야마 vs 오노사토"
		String nickName,        // 삭제된 경우 null
		String displayContent,
		String createdAt,
		boolean deleted,
		boolean blinded
) {
}
