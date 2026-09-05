package com.torikumilab.sumoarchive.domain.entity.constant;

/**
 * 댓글이 어떤 경로로 삭제되었는지 구분.
 * - USER  : 작성자가 비밀번호를 입력해 스스로 지운 경우 → "작성자에 의해 삭제된 댓글입니다"
 * - ADMIN : 관리자가 세션 권한으로 가림 처리한 경우 → "관리자에 의해 블라인드 처리된 댓글입니다"
 */
public enum DeletedBy {
	USER, ADMIN
}
