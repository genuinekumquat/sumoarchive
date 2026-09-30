package com.torikumilab.sumoarchive.service.exception;

/**
 * IP 기반 요청 빈도(Rate Limit) 초과 시 발생.
 * API 응답에서는 HTTP 429 (TOO_MANY_REQUESTS)로 변환된다.
 *
 * <p>댓글 쪽은 메시지 코드 + 인자(요청 언어로 번역), 관리자 로그인은 화면에 바로 쓰는 한국어 문장을 담는다.</p>
 */
public class RateLimitExceededException extends RuntimeException implements LocalizedMessage {

	private final transient Object[] args;

	public RateLimitExceededException(String message, Object... args) {
		super(message);
		this.args = args;
	}

	@Override
	public Object[] getMessageArgs() {
		return args;
	}
}
