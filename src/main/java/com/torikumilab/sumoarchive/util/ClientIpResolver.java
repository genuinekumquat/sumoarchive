package com.torikumilab.sumoarchive.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 프록시(Nginx, Cloudflare, AWS ALB 등) 환경 및 직접 연결 환경에서
 * 클라이언트의 실제 IP 주소를 안전하게 추출한다.
 */
public final class ClientIpResolver {

	private ClientIpResolver() {
	}

	public static String getClientIp(HttpServletRequest request) {
		if (request == null) {
			return "unknown";
		}
		String xForwardedFor = request.getHeader("X-Forwarded-For");
		if (xForwardedFor != null && !xForwardedFor.isBlank()) {
			return xForwardedFor.split(",")[0].trim();
		}
		String xRealIp = request.getHeader("X-Real-IP");
		if (xRealIp != null && !xRealIp.isBlank()) {
			return xRealIp.trim();
		}
		return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
	}
}
