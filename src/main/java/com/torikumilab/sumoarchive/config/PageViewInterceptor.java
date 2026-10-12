package com.torikumilab.sumoarchive.config;

import com.torikumilab.sumoarchive.service.stats.VisitClassifier;
import com.torikumilab.sumoarchive.service.stats.VisitStatsCollector;
import com.torikumilab.sumoarchive.util.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 공개 HTML 페이지 방문을 방문 통계(VisitStatsCollector)에 넘긴다. WebConfig에서 대상 경로만 등록한다.
 * 정상 응답(GET 200)만 세고, 봇·관리자로 로그인한 상태의 방문은 뺀다.
 * 수집기는 ObjectProvider로 받아 @WebMvcTest처럼 서비스 빈이 없는 테스트에서도 그냥 지나간다.
 */
public class PageViewInterceptor implements HandlerInterceptor {

	private final ObjectProvider<VisitStatsCollector> collector;

	public PageViewInterceptor(ObjectProvider<VisitStatsCollector> collector) {
		this.collector = collector;
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
		if (ex != null || !"GET".equals(request.getMethod()) || response.getStatus() != 200) {
			return;
		}
		HttpSession session = request.getSession(false);
		if (session != null && Boolean.TRUE.equals(session.getAttribute("isAdmin"))) {
			return;
		}
		String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
		if (VisitClassifier.isBot(userAgent, request.getHeader(HttpHeaders.ACCEPT_LANGUAGE))) {
			return;
		}
		VisitClassifier.Page page = VisitClassifier.classify(request.getRequestURI().substring(request.getContextPath().length()));
		VisitStatsCollector c = collector.getIfAvailable();
		if (page == null || c == null) {
			return;
		}
		String source = VisitClassifier.referrerSource(request.getHeader(HttpHeaders.REFERER), request.getServerName());
		c.record(page, ClientIpResolver.getClientIp(request), userAgent, source);
	}
}
