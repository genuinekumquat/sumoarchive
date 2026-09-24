package com.torikumilab.sumoarchive.service.security;

import com.torikumilab.sumoarchive.service.exception.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
				.hasMessageContaining("너무 빠르게 댓글을 작성하고 있습니다");
	}

	@Test
	@DisplayName("댓글 삭제: 5분 내 5회 비밀번호 오류 누적 시 삭제 시도 차단")
	void commentDelete_whenFailLimitReached_blocksFurtherAttempts() {
		String ip = "192.168.1.101";

		// 4회 실패 기록
		for (int i = 0; i < 4; i++) {
			rateLimiterService.recordCommentDeleteFailure(ip);
			assertThatCode(() -> rateLimiterService.checkCommentDeleteAllowed(ip))
					.doesNotThrowAnyException();
		}

		// 5회째 실패 기록
		rateLimiterService.recordCommentDeleteFailure(ip);

		// 이후 삭제 시도 시 차단
		assertThatThrownBy(() -> rateLimiterService.checkCommentDeleteAllowed(ip))
				.isInstanceOf(RateLimitExceededException.class)
				.hasMessageContaining("비밀번호 입력 시도가 너무 많습니다");
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
