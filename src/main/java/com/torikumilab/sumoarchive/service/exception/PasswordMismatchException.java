package com.torikumilab.sumoarchive.service.exception;

/**
 * 익명 댓글 삭제 시 입력한 4자리 비밀번호가 저장된 값과 다를 때.
 * API 계층에서 403으로 변환된다(ApiExceptionHandler).
 */
public class PasswordMismatchException extends RuntimeException {
	public PasswordMismatchException(String message) {
		super(message);
	}
}
