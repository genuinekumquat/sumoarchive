package com.torikumilab.sumoarchive.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 관리자 상태 변경 요청(POST 등)에 CSRF 토큰을 요구한다. GET/HEAD/OPTIONS는 통과.
 * 로그인 확인은 AdminAuthInterceptor가 먼저 하고, 여기서는 토큰만 본다 (WebConfig 등록 순서).
 * 로그인·로그아웃은 제외 - 로그인 전에는 토큰이 없고, 로그아웃은 위조돼도 피해가 없어서.
 */
public class AdminCsrfInterceptor implements HandlerInterceptor {

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		String method = request.getMethod();
		if ("GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method)) {
			return true;
		}
		if (AdminCsrf.isValid(request)) {
			return true;
		}
		response.sendError(HttpServletResponse.SC_FORBIDDEN, "CSRF 토큰이 없거나 올바르지 않습니다. 페이지를 새로고침한 뒤 다시 시도하세요.");
		return false;
	}
}
