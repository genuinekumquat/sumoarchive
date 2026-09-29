package com.torikumilab.sumoarchive.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 관리자 화면 CSRF 토큰. Spring Security 없이 세션에 토큰 하나를 두는 방식(synchronizer token).
 * 로그인 성공 시 발급하고, 관리자 POST 요청은 폼 숨김 필드(_csrf) 또는 헤더(X-CSRF-TOKEN)로 같은 값을 보내야 한다.
 * 폼 숨김 필드는 CsrfRequestDataValueProcessor가 th:action 폼마다 자동으로 넣는다.
 */
public final class AdminCsrf {

	public static final String SESSION_ATTR = "adminCsrfToken";
	public static final String PARAM = "_csrf";
	public static final String HEADER = "X-CSRF-TOKEN";

	private static final SecureRandom RANDOM = new SecureRandom();

	private AdminCsrf() {
	}

	/** 새 토큰을 만들어 세션에 저장하고 반환한다. */
	public static String issue(HttpSession session) {
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		session.setAttribute(SESSION_ATTR, token);
		return token;
	}

	/** 세션에 저장된 토큰 (없으면 null) */
	public static String current(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		return session == null ? null : (String) session.getAttribute(SESSION_ATTR);
	}

	/** 요청에 담긴 토큰이 세션 토큰과 같은지. 비교는 길이·내용과 무관하게 일정 시간이 걸리도록. */
	public static boolean isValid(HttpServletRequest request) {
		String expected = current(request);
		String actual = request.getHeader(HEADER);
		if (actual == null) {
			actual = request.getParameter(PARAM);
		}
		return expected != null && actual != null && constantTimeEquals(expected, actual);
	}

	public static boolean constantTimeEquals(String a, String b) {
		return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
	}
}
