package com.torikumilab.sumoarchive.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Rate Limiter용 클라이언트 IP.
 *
 * <p>X-Forwarded-For 헤더를 직접 읽지 않는다. 헤더는 누구나 임의로 보낼 수 있어서, 예전처럼 첫 값을 믿으면
 * 요청마다 값을 바꿔 로그인 시도·댓글 도배 제한을 피할 수 있었다. 대신 {@code server.forward-headers-strategy=native}
 * (Tomcat RemoteIpValve)가 직전 연결이 신뢰하는 프록시(사설망·localhost - Nginx, Docker 게이트웨이)일 때만
 * 헤더를 오른쪽부터 따라가 실제 IP를 {@code getRemoteAddr()}에 넣어준다. 인터넷에서 앱 포트로 직접 온 요청의 헤더는 무시된다.</p>
 */
public final class ClientIpResolver {

	private ClientIpResolver() {
	}

	public static String getClientIp(HttpServletRequest request) {
		if (request == null || request.getRemoteAddr() == null) {
			return "unknown";
		}
		return request.getRemoteAddr();
	}
}
