package com.torikumilab.sumoarchive.controller.api;

import com.torikumilab.sumoarchive.repository.RikishiRepository;
import com.torikumilab.sumoarchive.repository.TorikumiRepository;
import com.torikumilab.sumoarchive.service.stats.VisitClassifier;
import com.torikumilab.sumoarchive.service.stats.VisitStatsCollector;
import com.torikumilab.sumoarchive.util.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 방문 통계 beacon: 공개 페이지가 브라우저에서 열린 뒤 fragments/common :: pageView 스크립트가 보낸다.
 * 2026.10.28 전까지는 HTML 응답 때 서버에서 셌는데, HTML만 받아가고 JS·CSS는 안 불러오는 수집기(구버전 Chrome UA로
 * 전 세계 IP를 돌며 페이지 하나씩)가 하루 수백 명으로 잡혀서 브라우저가 스크립트를 실행한 경우만 센다.
 * 봇·관리자 제외 기준은 예전과 같고, 없는 리키시·경기 id는 버린다. 집계 실패가 화면에 영향을 주지 않도록 항상 204.
 */
@RestController
@RequiredArgsConstructor
public class PageViewApiController {

	private final VisitStatsCollector visitStatsCollector;
	private final RikishiRepository rikishiRepository;
	private final TorikumiRepository torikumiRepository;

	/**
	 * @param path 페이지 경로(location.pathname)
	 * @param ref  그 페이지의 document.referrer (외부 유입처 판별용, 없으면 빈 값)
	 */
	@PostMapping("/api/pv")
	public ResponseEntity<Void> record(@RequestParam String path, @RequestParam(required = false) String ref,
										HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		boolean admin = session != null && Boolean.TRUE.equals(session.getAttribute("isAdmin"));
		String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
		VisitClassifier.Page page = VisitClassifier.classify(path);
		if (admin || page == null || VisitClassifier.isBot(userAgent, request.getHeader(HttpHeaders.ACCEPT_LANGUAGE))
				|| !exists(page)) {
			return ResponseEntity.noContent().build();
		}
		String source = VisitClassifier.referrerSource(ref == null || ref.isBlank() ? null : ref, request.getServerName());
		visitStatsCollector.record(page, ClientIpResolver.getClientIp(request), userAgent, source);
		return ResponseEntity.noContent().build();
	}

	private boolean exists(VisitClassifier.Page page) {
		return switch (page.type()) {
			case RIKISHI -> rikishiRepository.existsById(page.targetId());
			case TORIKUMI -> torikumiRepository.existsById(page.targetId());
			default -> true;
		};
	}
}
