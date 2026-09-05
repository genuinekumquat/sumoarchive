package com.torikumilab.sumoarchive.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * /admin/** 경로를 세션 isAdmin=true 여부로 가드한다 (WebConfig에서 /admin/login, /admin/logout은 제외 등록).
 * TorikumiCommentApiController의 블라인드 API는 이 인터셉터 대상이 아니라 자체적으로
 * AdminOnlyException(401)로 세션을 검증하므로 별개다.
 */
public class AdminAuthInterceptor implements HandlerInterceptor {

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		HttpSession session = request.getSession(false);
		if (session != null && Boolean.TRUE.equals(session.getAttribute("isAdmin"))) {
			return true;
		}
		response.sendRedirect(request.getContextPath() + "/admin/login");
		return false;
	}
}
