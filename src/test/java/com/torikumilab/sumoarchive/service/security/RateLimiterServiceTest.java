package com.torikumilab.sumoarchive.service.security;

import com.torikumilab.sumoarchive.service.exception.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimiterServiceTest {

	private RateLimiterService rateLimiterService;

	@BeforeEach
	void setUp() {
		rateLimiterService = new RateLimiterService();
	}

	@Test
	@DisplayName("댓글 작성: 3초 쿨다운 이내 연속 작성 시 RateLimitExceededException 발생")
	void commentPost_whenWithinCooldown_throwsException() {
		String ip = "192.168.1.100";

		// 첫 번째 작성 성공
		assertThatCode(() -> rateLimiterService.checkCommentPostAllowed(ip))
				.doesNotThrowAnyException();

		// 3초 이내 즉시 두 번째 작성 시도 시 차단
		assertThatThrownBy(() -> rateLimiterService.checkCommentPostAllowed(ip))
				.isInstanceOf(RateLimitExceededException.class)
				.hasMessage("comment.error.post.cooldown");
	}

	@Test
	@DisplayName("댓글 삭제: 5분 내 5회 비밀번호 오류 누적 시 삭제 시도 차단")
	void commentDelete_whenFailLimitReached_blocksFurtherAttempts() {
		String ip = "192.168.1.101";
		Integer commentId = 1;

		// 4회 실패 기록
		for (int i = 0; i < 4; i++) {
			rateLimiterService.recordCommentDeleteFailure(ip, commentId);
			assertThatCode(() -> rateLimiterService.checkCommentDeleteAllowed(ip, commentId))
					.doesNotThrowAnyException();
		}

		// 5회째 실패 기록
		rateLimiterService.recordCommentDeleteFailure(ip, commentId);

		// 이후 삭제 시도 시 차단 (다른 댓글이어도 같은 IP면 차단)
		assertThatThrownBy(() -> rateLimiterService.checkCommentDeleteAllowed(ip, 2))
				.isInstanceOf(RateLimitExceededException.class)
				.hasMessage("comment.error.delete.ip");
	}

	@Test
	@DisplayName("댓글 삭제: IP를 바꿔 가며 한 댓글에 10회 틀리면 어느 IP에서든 그 댓글 삭제 차단")
	void commentDelete_whenOneCommentFailsTenTimesAcrossIps_locksThatComment() {
		Integer target = 10;

		// IP 10개에서 한 번씩 (IP별 한도 5회에는 걸리지 않음)
		for (int i = 0; i < 10; i++) {
			String ip = "10.0.0." + i;
			assertThatCode(() -> rateLimiterService.checkCommentDeleteAllowed(ip, target))
					.doesNotThrowAnyException();
			rateLimiterService.recordCommentDeleteFailure(ip, target);
		}

		// 처음 보는 IP여도 그 댓글은 잠김
		assertThatThrownBy(() -> rateLimiterService.checkCommentDeleteAllowed("10.0.1.1", target))
				.isInstanceOf(RateLimitExceededException.class)
				.hasMessage("comment.error.delete.locked");

		// 다른 댓글은 영향 없음
		assertThatCode(() -> rateLimiterService.checkCommentDeleteAllowed("10.0.1.1", 11))
				.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("댓글 삭제: 자기 댓글 삭제 성공으로 IP 실패 기록을 초기화해도 댓글별 잠금은 유지")
	void commentDelete_whenIpCounterResetBySuccess_commentLockStillApplies() {
		String ip = "192.168.1.104";
		Integer target = 20;

		// 4회 틀리고 → 자기 댓글 삭제 성공으로 IP 기록 초기화, 를 반복해 10회 누적
		for (int i = 0; i < 10; i++) {
			rateLimiterService.recordCommentDeleteFailure(ip, target);
			if (i % 4 == 3) {
				rateLimiterService.recordCommentDeleteSuccess(ip);
			}
		}
		rateLimiterService.recordCommentDeleteSuccess(ip);

		assertThatThrownBy(() -> rateLimiterService.checkCommentDeleteAllowed(ip, target))
				.isInstanceOf(RateLimitExceededException.class)
				.hasMessage("comment.error.delete.locked");
	}

	@Test
	@DisplayName("댓글 신고: 같은 IP가 같은 댓글을 다시 신고하면 세지 않는다 (다른 IP·다른 댓글은 셈)")
	void commentReport_whenSameIpReportsSameCommentAgain_isIgnored() {
		String ip = "192.168.1.105";

		assertThat(rateLimiterService.tryRecordCommentReport(ip, 30)).isTrue();
		assertThat(rateLimiterService.tryRecordCommentReport(ip, 30)).isFalse();

		assertThat(rateLimiterService.tryRecordCommentReport("192.168.1.106", 30)).isTrue();
		assertThat(rateLimiterService.tryRecordCommentReport(ip, 31)).isTrue();
	}

	@Test
	@DisplayName("댓글 신고: 한 IP가 10분 내 10건을 넘게 신고하면 차단")
	void commentReport_whenOverLimit_throwsException() {
		String ip = "192.168.1.107";

		for (int i = 0; i < 10; i++) {
			assertThat(rateLimiterService.tryRecordCommentReport(ip, 100 + i)).isTrue();
		}

		assertThatThrownBy(() -> rateLimiterService.tryRecordCommentReport(ip, 200))
				.isInstanceOf(RateLimitExceededException.class)
				.hasMessage("comment.error.report.limit");

		// 이미 신고한 댓글 재신고는 한도와 상관없이 조용히 무시
		assertThat(rateLimiterService.tryRecordCommentReport(ip, 100)).isFalse();
	}

	@Test
	@DisplayName("관리자 로그인: 5회 연속 실패 시 5분간 로그인 차단")
	void adminLogin_whenFiveFailures_blocksLogin() {
		String ip = "192.168.1.102";

		for (int i = 0; i < 5; i++) {
			rateLimiterService.recordAdminLoginFailure(ip);
		}

		assertThatThrownBy(() -> rateLimiterService.checkAdminLoginAllowed(ip))
				.isInstanceOf(RateLimitExceededException.class)
				.hasMessageContaining("로그인 실패 횟수(5회)를 초과했습니다");
	}

	@Test
	@DisplayName("관리자 로그인: 로그인 성공 시 실패 카운트가 초기화되어야 한다")
	void adminLogin_whenSuccess_clearsFailures() {
		String ip = "192.168.1.103";

		// 4회 실패
		for (int i = 0; i < 4; i++) {
			rateLimiterService.recordAdminLoginFailure(ip);
		}

		// 1회 성공
		rateLimiterService.recordAdminLoginSuccess(ip);

		// 실패 카운트가 리셋되어 다시 허용됨
		assertThatCode(() -> rateLimiterService.checkAdminLoginAllowed(ip))
				.doesNotThrowAnyException();
	}
}
