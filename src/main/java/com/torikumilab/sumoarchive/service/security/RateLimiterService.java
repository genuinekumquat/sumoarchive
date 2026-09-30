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
 * 댓글 쪽 예외 메시지는 메시지 코드(comment.error.*)라 API 응답에서 요청 언어로 바뀐다.
 * 관리자 로그인은 관리자 화면(한국어)에 바로 쓰므로 문장 그대로 둔다.
 * 외부 의존성(Redis 등) 없이 단일 인스턴스/컨테이너 환경에서 초고속으로 작동합니다.
 *
 * 1) 익명 댓글 작성: 3초 쿨다운 & 1분당 최대 5건 (도배 방지)
 * 2) 익명 댓글 삭제: 5분 내 최대 5회 비밀번호 오류 허용 (4자리 PIN 무차별 대입 방어)
 *    + 댓글 하나당 1시간 내 10회 오류 시 그 댓글 삭제를 잠금 (IP를 바꿔 가며 시도하는 경우 방어)
 * 3) 관리자 로그인: 5분 내 최대 5회 실패 허용 (무차별 대입 방어)
 * 4) 댓글 신고: 같은 IP의 같은 댓글 중복 신고는 24시간 동안 무시, IP당 10분에 10건까지 (신고 도배 방지)
 */
@Service
public class RateLimiterService {

	private final Map<String, Deque<Long>> commentPostHistory = new ConcurrentHashMap<>();
	private final Map<String, Deque<Long>> commentDeleteFailHistory = new ConcurrentHashMap<>();
	private final Map<Integer, Deque<Long>> commentPinFailHistory = new ConcurrentHashMap<>();
	private final Map<String, Deque<Long>> adminLoginFailHistory = new ConcurrentHashMap<>();
	private final Map<String, Deque<Long>> commentReportHistory = new ConcurrentHashMap<>();
	// "IP:댓글ID" -> 신고 시각. 신고자 정보는 DB에 남기지 않으므로 중복 신고는 여기서만 막는다(재시작 시 초기화).
	private final Map<String, Long> reportedByIp = new ConcurrentHashMap<>();

	private static final long COMMENT_COOLDOWN_MILLIS = 3_000;      // 3초 최소 간격
	private static final long COMMENT_POST_WINDOW_MILLIS = 60_000;  // 1분
	private static final int COMMENT_POST_MAX = 5;                  // 1분당 5회

	private static final long FAIL_WINDOW_MILLIS = 300_000;         // 5분
	private static final int FAIL_MAX = 5;                          // 5분 내 5회 실패

	// 댓글 단위 잠금: IP 제한만으로는 IP를 여러 개 쓰거나, 자기 댓글을 지워 IP 실패 기록을 초기화하면서
	// 1만 개 조합을 다 시도할 수 있다. 댓글 하나에는 IP와 상관없이 1시간에 10번까지만 틀릴 수 있게 한다
	// (다 맞히려면 평균 500시간). 작성자 본인도 잠금 동안은 못 지우므로 관리자 블라인드로 처리한다.
	private static final long COMMENT_LOCK_WINDOW_MILLIS = 3_600_000; // 1시간
	private static final int COMMENT_LOCK_FAIL_MAX = 10;               // 1시간 내 10회 실패

	private static final long REPORT_WINDOW_MILLIS = 600_000;          // 10분
	private static final int REPORT_MAX = 10;                          // 10분당 10건
	private static final long REPORT_DEDUP_MILLIS = 86_400_000;        // 같은 댓글 재신고 무시 24시간

	// ===== 1. 댓글 작성 =====

	public synchronized void checkCommentPostAllowed(String clientIp) {
		long now = System.currentTimeMillis();
		Deque<Long> history = commentPostHistory.computeIfAbsent(clientIp, k -> new ArrayDeque<>());

		if (!history.isEmpty()) {
			long last = history.peekLast();
			if (now - last < COMMENT_COOLDOWN_MILLIS) {
				long waitSec = (COMMENT_COOLDOWN_MILLIS - (now - last) + 999) / 1000;
				throw new RateLimitExceededException("comment.error.post.cooldown", waitSec);
			}
		}

		while (!history.isEmpty() && now - history.peekFirst() > COMMENT_POST_WINDOW_MILLIS) {
			history.pollFirst();
		}

		if (history.size() >= COMMENT_POST_MAX) {
			throw new RateLimitExceededException("comment.error.post.limit", COMMENT_POST_MAX);
		}

		history.addLast(now);
	}

	// ===== 2. 댓글 삭제 (PIN 무차별 대입 방어) =====

	public synchronized void checkCommentDeleteAllowed(String clientIp, Integer commentId) {
		long now = System.currentTimeMillis();
		Deque<Long> fails = commentDeleteFailHistory.computeIfAbsent(clientIp, k -> new ArrayDeque<>());

		while (!fails.isEmpty() && now - fails.peekFirst() > FAIL_WINDOW_MILLIS) {
			fails.pollFirst();
		}

		if (fails.size() >= FAIL_MAX) {
			throw new RateLimitExceededException("comment.error.delete.ip");
		}

		Deque<Long> commentFails = commentPinFailHistory.computeIfAbsent(commentId, k -> new ArrayDeque<>());
		while (!commentFails.isEmpty() && now - commentFails.peekFirst() > COMMENT_LOCK_WINDOW_MILLIS) {
			commentFails.pollFirst();
		}

		if (commentFails.size() >= COMMENT_LOCK_FAIL_MAX) {
			throw new RateLimitExceededException("comment.error.delete.locked");
		}
	}

	public synchronized void recordCommentDeleteFailure(String clientIp, Integer commentId) {
		long now = System.currentTimeMillis();
		Deque<Long> fails = commentDeleteFailHistory.computeIfAbsent(clientIp, k -> new ArrayDeque<>());
		fails.addLast(now);
		commentPinFailHistory.computeIfAbsent(commentId, k -> new ArrayDeque<>()).addLast(now);
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

	// ===== 4. 댓글 신고 (중복·도배 방지) =====

	/**
	 * 신고를 받아도 되는지 확인하고 받으면 기록한다.
	 * @return 새 신고면 true, 같은 IP가 이미 신고한 댓글이면 false (조용히 무시할 신고)
	 * @throws RateLimitExceededException IP당 신고 한도 초과
	 */
	public synchronized boolean tryRecordCommentReport(String clientIp, Integer commentId) {
		long now = System.currentTimeMillis();
		String key = clientIp + ":" + commentId;
		Long reportedAt = reportedByIp.get(key);
		if (reportedAt != null && now - reportedAt <= REPORT_DEDUP_MILLIS) {
			return false;
		}

		Deque<Long> history = commentReportHistory.computeIfAbsent(clientIp, k -> new ArrayDeque<>());
		while (!history.isEmpty() && now - history.peekFirst() > REPORT_WINDOW_MILLIS) {
			history.pollFirst();
		}
		if (history.size() >= REPORT_MAX) {
			throw new RateLimitExceededException("comment.error.report.limit");
		}

		history.addLast(now);
		reportedByIp.put(key, now);
		return true;
	}

	// ===== 주기적 메모리 청소 (10분마다 실행) =====

	@Scheduled(fixedRate = 600_000)
	public synchronized void pruneExpiredEntries() {
		long now = System.currentTimeMillis();
		pruneMap(commentPostHistory, now, COMMENT_POST_WINDOW_MILLIS);
		pruneMap(commentDeleteFailHistory, now, FAIL_WINDOW_MILLIS);
		pruneMap(commentPinFailHistory, now, COMMENT_LOCK_WINDOW_MILLIS);
		pruneMap(adminLoginFailHistory, now, FAIL_WINDOW_MILLIS);
		pruneMap(commentReportHistory, now, REPORT_WINDOW_MILLIS);
		reportedByIp.values().removeIf(at -> now - at > REPORT_DEDUP_MILLIS);
	}

	private <K> void pruneMap(Map<K, Deque<Long>> map, long now, long window) {
		map.entrySet().removeIf(entry -> {
			Deque<Long> q = entry.getValue();
			while (!q.isEmpty() && now - q.peekFirst() > window) {
				q.pollFirst();
			}
			return q.isEmpty();
		});
	}
}
