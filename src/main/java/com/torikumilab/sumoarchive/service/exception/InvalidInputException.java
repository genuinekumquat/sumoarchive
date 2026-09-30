package com.torikumilab.sumoarchive.service.exception;

/**
 * 사용자 입력 검증 실패. 메시지는 메시지 코드이고 API 응답에서 400 + 요청 언어의 문장으로 바뀐다
 * (IllegalArgumentException 처리와 같은 경로 - ApiExceptionHandler).
 */
public class InvalidInputException extends IllegalArgumentException implements LocalizedMessage {

	private final transient Object[] args;

	public InvalidInputException(String messageCode, Object... args) {
		super(messageCode);
		this.args = args;
	}

	@Override
	public Object[] getMessageArgs() {
		return args;
	}
}
