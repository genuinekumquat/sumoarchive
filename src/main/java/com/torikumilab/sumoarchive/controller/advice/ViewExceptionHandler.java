package com.torikumilab.sumoarchive.controller.advice;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 뷰 컨트롤러에서 던져진 EntityNotFoundException을 500 대신 404 페이지로 연결한다.
 * (RikishiDetailController에 남아 있던 TODO를 여기서 해결 — 리키시/토리쿠미 상세 둘 다 커버)
 *
 * <p>controller.api 쪽은 ApiExceptionHandler(더 높은 우선순위)가 JSON으로 먼저 처리하므로
 * 이 어드바이스는 실질적으로 화면 컨트롤러에만 적용된다.</p>
 */
@Order(Ordered.LOWEST_PRECEDENCE)
@ControllerAdvice(basePackages = "com.torikumilab.sumoarchive.controller")
public class ViewExceptionHandler {

	@ExceptionHandler(EntityNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public String handleNotFound(EntityNotFoundException e, Model model) {
		model.addAttribute("message", e.getMessage());
		return "error/404";
	}
}
