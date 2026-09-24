package com.torikumilab.sumoarchive.service.exception;

/**
 * IP 기반 요청 빈도(Rate Limit) 초과 시 발생.
 * API 응답에서는 HTTP 429 (TOO_MANY_REQUESTS)로 변환된다.
 */
public class RateLimitExceededException extends RuntimeException {
	public RateLimitExceededException(String message) {
		super(message);
	}
}
