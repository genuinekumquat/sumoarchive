package com.torikumilab.sumoarchive.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.support.RequestDataValueProcessor;

import java.util.Map;

/**
 * Thymeleaf가 th:action 폼을 그릴 때마다 호출하는 훅. 관리자 세션에 CSRF 토큰이 있으면
 * 숨김 필드(_csrf)를 자동으로 붙인다 (Spring Security가 폼에 토큰을 넣는 방식과 같음).
 * 그래서 관리자 템플릿의 폼마다 토큰 입력을 따로 적지 않아도 된다.
 * 빈 이름이 반드시 "requestDataValueProcessor"여야 Spring MVC가 찾는다 (WebConfig).
 */
public class CsrfRequestDataValueProcessor implements RequestDataValueProcessor {

	@Override
	public String processAction(HttpServletRequest request, String action, String httpMethod) {
		return action;
	}

	@Override
	public String processFormFieldValue(HttpServletRequest request, String name, String value, String type) {
		return value;
	}

	@Override
	public Map<String, String> getExtraHiddenFields(HttpServletRequest request) {
		String token = AdminCsrf.current(request);
		return token == null ? Map.of() : Map.of(AdminCsrf.PARAM, token);
	}

	@Override
	public String processUrl(HttpServletRequest request, String url) {
		return url;
	}
}
