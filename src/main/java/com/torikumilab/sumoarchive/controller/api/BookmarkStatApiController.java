package com.torikumilab.sumoarchive.controller.api;

import com.torikumilab.sumoarchive.repository.RikishiRepository;
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
 * 즐겨찾기 ☆를 누르거나 풀 때 bookmark.js가 보내는 "이 리키시 +1/−1" (관리자 방문 통계의 「즐겨찾기 많은 리키시」).
 * 목록은 계속 브라우저에만 있고, 여기엔 리키시 id와 추가/해제만 온다. 방문 통계와 같이 봇·관리자는 세지 않고,
 * 없는 리키시 id는 버린다. 집계 실패가 화면에 영향을 주지 않도록 항상 204.
 */
@RestController
@RequiredArgsConstructor
public class BookmarkStatApiController {

	private final VisitStatsCollector visitStatsCollector;
	private final RikishiRepository rikishiRepository;

	@PostMapping("/api/bookmark-stat")
	public ResponseEntity<Void> record(@RequestParam Integer rikishiId, @RequestParam boolean added,
										HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		boolean admin = session != null && Boolean.TRUE.equals(session.getAttribute("isAdmin"));
		String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
		if (!admin && !VisitClassifier.isBot(userAgent, request.getHeader(HttpHeaders.ACCEPT_LANGUAGE))
				&& rikishiRepository.existsById(rikishiId)) {
			visitStatsCollector.recordBookmark(rikishiId, added, ClientIpResolver.getClientIp(request), userAgent);
		}
		return ResponseEntity.noContent().build();
	}
}
