package com.torikumilab.sumoarchive.service.security;

import com.torikumilab.sumoarchive.service.exception.RateLimitExceededException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 인메모리 슬라이딩 윈도우 기반 Rate Limiter (IP 쓰로틀링).
 * 외부 의존성(Redis 등) 없이 단일 인스턴스/컨테이너 환경에서 초고속으로 작동합니다.
 *
 * 1) 익명 댓글 작성: 3초 쿨다운 & 1분당 최대 5건 (도배 방지)
 * 2) 익명 댓글 삭제: 5분 내 최대 5회 비밀번호 오류 허용 (4자리 PIN 무차별 대입 방어)
 * 3) 관리자 로그인: 5분 내 최대 5회 실패 허용 (무차별 대입 방어)
 */
@Service
public class RateLimiterService {

	private final Map<String, Deque<Long>> commentPostHistory = new ConcurrentHashMap<>();
	private final Map<String, Deque<Long>> commentDeleteFailHistory = new ConcurrentHashMap<>();
	private final Map<String, Deque<Long>> adminLoginFailHistory = new ConcurrentHashMap<>();

	private static final long COMMENT_COOLDOWN_MILLIS = 3_000;      // 3초 최소 간격
	private static final long COMMENT_POST_WINDOW_MILLIS = 60_000;  // 1분
	private static final int COMMENT_POST_MAX = 5;                  // 1분당 5회

	private static final long FAIL_WINDOW_MILLIS = 300_000;         // 5분
	private static final int FAIL_MAX = 5;                          // 5분 내 5회 실패

	// ===== 1. 댓글 작성 =====

	public synchronized void checkCommentPostAllowed(String clientIp) {
		long now = System.currentTimeMillis();
		Deque<Long> history = commentPostHistory.computeIfAbsent(clientIp, k -> new ArrayDeque<>());

		if (!history.isEmpty()) {
			long last = history.peekLast();
			if (now - last < COMMENT_COOLDOWN_MILLIS) {
				long waitSec = (COMMENT_COOLDOWN_MILLIS - (now - last) + 999) / 1000;
				throw new RateLimitExceededException("너무 빠르게 댓글을 작성하고 있습니다. " + waitSec + "초 후 다시 시도해 주세요.");
			}
		}

		while (!history.isEmpty() && now - history.peekFirst() > COMMENT_POST_WINDOW_MILLIS) {
			history.pollFirst();
		}

		if (history.size() >= COMMENT_POST_MAX) {
			throw new RateLimitExceededException("댓글 작성 한도(1분당 " + COMMENT_POST_MAX + "건)를 초과했습니다. 잠시 후 다시 시도해 주세요.");
		}

		history.addLast(now);
	}

	// ===== 2. 댓글 삭제 (PIN 무차별 대입 방어) =====

	public synchronized void checkCommentDeleteAllowed(String clientIp) {
		long now = System.currentTimeMillis();
		Deque<Long> fails = commentDeleteFailHistory.computeIfAbsent(clientIp, k -> new ArrayDeque<>());

		while (!fails.isEmpty() && now - fails.peekFirst() > FAIL_WINDOW_MILLIS) {
			fails.pollFirst();
		}

		if (fails.size() >= FAIL_MAX) {
			throw new RateLimitExceededException("비밀번호 입력 시도가 너무 많습니다. 5분 후 다시 시도해 주세요.");
		}
	}

	public synchronized void recordCommentDeleteFailure(String clientIp) {
		long now = System.currentTimeMillis();
		Deque<Long> fails = commentDeleteFailHistory.computeIfAbsent(clientIp, k -> new ArrayDeque<>());
		fails.addLast(now);
	}

	public synchronized void recordCommentDeleteSuccess(String clientIp) {
		commentDeleteFailHistory.remove(clientIp);
	}

	// ===== 3. 관리자 로그인 (브루트포스 방어) =====

	public synchronized void checkAdminLoginAllowed(String clientIp) {
		long now = System.currentTimeMillis();
		Deque<Long> fails = adminLoginFailHistory.computeIfAbsent(clientIp, k -> new ArrayDeque<>());

		while (!fails.isEmpty() && now - fails.peekFirst() > FAIL_WINDOW_MILLIS) {
			fails.pollFirst();
		}

		if (fails.size() >= FAIL_MAX) {
			throw new RateLimitExceededException("로그인 실패 횟수(5회)를 초과했습니다. 5분 후 다시 시도해 주세요.");
		}
	}

	public synchronized void recordAdminLoginFailure(String clientIp) {
		long now = System.currentTimeMillis();
		Deque<Long> fails = adminLoginFailHistory.computeIfAbsent(clientIp, k -> new ArrayDeque<>());
		fails.addLast(now);
	}

	public synchronized void recordAdminLoginSuccess(String clientIp) {
		adminLoginFailHistory.remove(clientIp);
	}

	// ===== 주기적 메모리 청소 (10분마다 실행) =====

	@Scheduled(fixedRate = 600_000)
	public synchronized void pruneExpiredEntries() {
		long now = System.currentTimeMillis();
		pruneMap(commentPostHistory, now, COMMENT_POST_WINDOW_MILLIS);
		pruneMap(commentDeleteFailHistory, now, FAIL_WINDOW_MILLIS);
		pruneMap(adminLoginFailHistory, now, FAIL_WINDOW_MILLIS);
	}

	private void pruneMap(Map<String, Deque<Long>> map, long now, long window) {
		map.entrySet().removeIf(entry -> {
			Deque<Long> q = entry.getValue();
			while (!q.isEmpty() && now - q.peekFirst() > window) {
				q.pollFirst();
			}
			return q.isEmpty();
		});
	}
}
