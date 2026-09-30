package com.torikumilab.sumoarchive.service.exception;

/**
 * 예외 메시지(getMessage)가 사용자에게 보일 문장이 아니라 메시지 코드(messages*.properties)일 때,
 * 그 코드에 끼울 인자를 함께 넘긴다. ApiExceptionHandler가 요청 언어로 문장을 만든다.
 *
 * <p>코드가 아닌 일반 문장을 담은 예외는 번역 없이 그대로 보인다(관리자 화면 메시지 등).</p>
 */
public interface LocalizedMessage {

	Object[] getMessageArgs();
}
