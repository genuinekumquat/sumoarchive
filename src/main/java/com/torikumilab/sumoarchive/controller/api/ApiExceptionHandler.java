package com.torikumilab.sumoarchive.controller.api;

import com.torikumilab.sumoarchive.service.exception.AdminOnlyException;
import com.torikumilab.sumoarchive.service.exception.PasswordMismatchException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * controller.api 패키지 전용 예외 → JSON 변환. 프론트 fetch는 응답 코드로 분기하고
 * 본문의 message를 그대로 사용자에게 노출한다.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.torikumilab.sumoarchive.controller.api")
public class ApiExceptionHandler {

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException e) {
		return body(HttpStatus.BAD_REQUEST, e.getMessage());
	}

	@ExceptionHandler(PasswordMismatchException.class)
	public ResponseEntity<Map<String, String>> forbidden(PasswordMismatchException e) {
		return body(HttpStatus.FORBIDDEN, e.getMessage());
	}

	@ExceptionHandler(AdminOnlyException.class)
	public ResponseEntity<Map<String, String>> unauthorized(AdminOnlyException e) {
		return body(HttpStatus.UNAUTHORIZED, e.getMessage());
	}

	@ExceptionHandler(EntityNotFoundException.class)
	public ResponseEntity<Map<String, String>> notFound(EntityNotFoundException e) {
		return body(HttpStatus.NOT_FOUND, e.getMessage());
	}

	private ResponseEntity<Map<String, String>> body(HttpStatus status, String message) {
		return ResponseEntity.status(status).body(Map.of(
				"message", message != null ? message : status.getReasonPhrase()
		));
	}
}
