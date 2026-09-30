package com.torikumilab.sumoarchive.controller.api;

import com.torikumilab.sumoarchive.service.exception.AdminOnlyException;
import com.torikumilab.sumoarchive.service.exception.LocalizedMessage;
import com.torikumilab.sumoarchive.service.exception.PasswordMismatchException;
import com.torikumilab.sumoarchive.service.exception.RateLimitExceededException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Locale;
import java.util.Map;

/**
 * controller.api 패키지 전용 예외 → JSON 변환. 프론트 fetch는 응답 코드로 분기하고
 * 본문의 message를 그대로 사용자에게 노출한다.
 *
 * <p>예외 메시지가 메시지 코드(comment.error.* 등)면 요청 언어(세션의 lang)로 문장을 만들고,
 * 코드가 아닌 일반 문장이면 그대로 내보낸다.</p>
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.torikumilab.sumoarchive.controller.api")
@RequiredArgsConstructor
public class ApiExceptionHandler {

	private final MessageSource messageSource;

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException e, Locale locale) {
		return body(HttpStatus.BAD_REQUEST, e, locale);
	}

	@ExceptionHandler(PasswordMismatchException.class)
	public ResponseEntity<Map<String, String>> forbidden(PasswordMismatchException e, Locale locale) {
		return body(HttpStatus.FORBIDDEN, e, locale);
	}

	@ExceptionHandler(AdminOnlyException.class)
	public ResponseEntity<Map<String, String>> unauthorized(AdminOnlyException e, Locale locale) {
		return body(HttpStatus.UNAUTHORIZED, e, locale);
	}

	@ExceptionHandler(RateLimitExceededException.class)
	public ResponseEntity<Map<String, String>> tooManyRequests(RateLimitExceededException e, Locale locale) {
		return body(HttpStatus.TOO_MANY_REQUESTS, e, locale);
	}

	@ExceptionHandler(EntityNotFoundException.class)
	public ResponseEntity<Map<String, String>> notFound(EntityNotFoundException e, Locale locale) {
		return body(HttpStatus.NOT_FOUND, e, locale);
	}

	private ResponseEntity<Map<String, String>> body(HttpStatus status, RuntimeException e, Locale locale) {
		String message = e.getMessage();
		if (message == null) {
			message = status.getReasonPhrase();
		} else {
			Object[] args = e instanceof LocalizedMessage lm ? lm.getMessageArgs() : null;
			// 코드가 없으면(일반 문장) 기본값으로 원문을 그대로 쓴다
			message = messageSource.getMessage(message, args, message, locale);
		}
		return ResponseEntity.status(status).body(Map.of("message", message));
	}
}
