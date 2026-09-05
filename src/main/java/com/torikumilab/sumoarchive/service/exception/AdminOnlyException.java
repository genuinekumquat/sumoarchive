package com.torikumilab.sumoarchive.service.exception;

/**
 * 관리자 세션이 아닌데 관리자 전용 동작(댓글 블라인드 등)을 호출했을 때.
 * API 계층에서 401로 변환된다(ApiExceptionHandler).
 */
public class AdminOnlyException extends RuntimeException {
	public AdminOnlyException(String message) {
		super(message);
	}
}
