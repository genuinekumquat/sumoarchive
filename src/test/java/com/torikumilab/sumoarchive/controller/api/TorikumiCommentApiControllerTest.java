package com.torikumilab.sumoarchive.controller.api;

import com.torikumilab.sumoarchive.config.AdminCsrf;
import com.torikumilab.sumoarchive.service.CommentService;
import com.torikumilab.sumoarchive.service.exception.PasswordMismatchException;
import com.torikumilab.sumoarchive.service.exception.RateLimitExceededException;
import com.torikumilab.sumoarchive.service.security.RateLimiterService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 댓글 API의 요청 → 응답 코드 → 권한 검사 흐름. 서비스·Rate Limiter는 가짜로 두고,
 * WebConfig의 인터셉터(관리자 CSRF)와 ApiExceptionHandler는 실제 것을 쓴다.
 */
@WebMvcTest(TorikumiCommentApiController.class)
class TorikumiCommentApiControllerTest {

	private static final String BASE = "/api/torikumi/100/comments";
	private static final String CLIENT_IP = "203.0.113.5";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CommentService commentService;
	@MockitoBean
	private RateLimiterService rateLimiterService;

	// ===== 예외 → 응답 코드 (ApiExceptionHandler) =====

	@Test
	@DisplayName("작성: 입력 오류는 400과 message")
	void create_whenInvalidInput_returns400() throws Exception {
		given(commentService.addComment(anyInt(), any(), any(), any()))
				.willThrow(new IllegalArgumentException("닉네임을 입력해 주세요."));

		mockMvc.perform(post(BASE).param("nickname", "").param("password", "1234").param("content", "내용"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("닉네임을 입력해 주세요."));
	}

	@Test
	@DisplayName("작성: 정상이면 201과 갱신된 목록, 도배 한도 초과면 429이고 저장하지 않음")
	void create_successAndRateLimit() throws Exception {
		given(commentService.addComment(anyInt(), any(), any(), any())).willReturn(List.of());
		mockMvc.perform(post(BASE).param("nickname", "팬").param("password", "1234").param("content", "내용"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$").isArray());

		willThrow(new RateLimitExceededException("너무 빠르게 댓글을 작성하고 있습니다."))
				.given(rateLimiterService).checkCommentPostAllowed(any());
		mockMvc.perform(post(BASE).param("nickname", "팬").param("password", "1234").param("content", "내용"))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.message").value(containsString("너무 빠르게")));
		verify(commentService).addComment(anyInt(), any(), any(), any()); // 첫 요청 1번뿐
	}

	@Test
	@DisplayName("없는 댓글이면 404와 message")
	void delete_whenCommentMissing_returns404() throws Exception {
		given(commentService.deleteByUser(100, 7, "1234"))
				.willThrow(new EntityNotFoundException("댓글을 찾을 수 없습니다. id=7"));

		mockMvc.perform(post(BASE + "/7/delete").param("password", "1234"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value(containsString("댓글을 찾을 수 없습니다")));
	}

	// ===== 본인 삭제 ↔ Rate Limiter 연결 =====

	@Test
	@DisplayName("삭제: 비밀번호가 틀리면 403, 그 IP·댓글로 실패가 기록된다")
	void delete_whenWrongPin_returns403AndRecordsFailure() throws Exception {
		given(commentService.deleteByUser(100, 7, "9999"))
				.willThrow(new PasswordMismatchException("비밀번호가 일치하지 않습니다."));

		mockMvc.perform(post(BASE + "/7/delete").param("password", "9999")
						.with(req -> { req.setRemoteAddr(CLIENT_IP); return req; }))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("비밀번호가 일치하지 않습니다."));

		verify(rateLimiterService).checkCommentDeleteAllowed(CLIENT_IP, 7);
		verify(rateLimiterService).recordCommentDeleteFailure(CLIENT_IP, 7);
		verify(rateLimiterService, never()).recordCommentDeleteSuccess(any());
	}

	@Test
	@DisplayName("삭제: 성공하면 IP 실패 기록을 지우고 실패는 기록하지 않는다")
	void delete_whenCorrectPin_recordsSuccess() throws Exception {
		given(commentService.deleteByUser(100, 7, "1234")).willReturn(List.of());

		mockMvc.perform(post(BASE + "/7/delete").param("password", "1234")
						.with(req -> { req.setRemoteAddr(CLIENT_IP); return req; }))
				.andExpect(status().isOk());

		verify(rateLimiterService).recordCommentDeleteSuccess(CLIENT_IP);
		verify(rateLimiterService, never()).recordCommentDeleteFailure(any(), any());
	}

	@Test
	@DisplayName("삭제: 잠금(429) 상태면 비밀번호 확인까지 가지 않는다")
	void delete_whenLocked_returns429WithoutCheckingPin() throws Exception {
		willThrow(new RateLimitExceededException("이 댓글은 비밀번호 오류가 너무 많아 잠시 삭제할 수 없습니다."))
				.given(rateLimiterService).checkCommentDeleteAllowed(any(), any());

		mockMvc.perform(post(BASE + "/7/delete").param("password", "1234"))
				.andExpect(status().isTooManyRequests());

		verify(commentService, never()).deleteByUser(any(), any(), any());
	}

	// ===== 관리자 블라인드: CSRF 인터셉터 → 컨트롤러의 관리자 세션 확인 =====

	@Test
	@DisplayName("블라인드: CSRF 토큰이 없으면 403 (인터셉터에서 막힘)")
	void blind_withoutCsrfToken_returns403() throws Exception {
		MockHttpSession session = adminSession();

		mockMvc.perform(post(BASE + "/7/blind").session(session))
				.andExpect(status().isForbidden());

		verify(commentService, never()).blindByAdmin(any(), any());
	}

	@Test
	@DisplayName("블라인드: 토큰은 맞아도 관리자 세션이 아니면 401")
	void blind_withoutAdminSession_returns401() throws Exception {
		MockHttpSession session = new MockHttpSession();
		String token = AdminCsrf.issue(session);

		mockMvc.perform(post(BASE + "/7/blind").session(session).header(AdminCsrf.HEADER, token))
				.andExpect(status().isUnauthorized());

		verify(commentService, never()).blindByAdmin(any(), any());
	}

	@Test
	@DisplayName("블라인드: 관리자 세션 + CSRF 토큰이면 처리된다")
	void blind_withAdminSessionAndToken_succeeds() throws Exception {
		MockHttpSession session = adminSession();
		String token = (String) session.getAttribute(AdminCsrf.SESSION_ATTR);
		given(commentService.blindByAdmin(100, 7)).willReturn(List.of());

		mockMvc.perform(post(BASE + "/7/blind").session(session).header(AdminCsrf.HEADER, token))
				.andExpect(status().isOk());

		verify(commentService).blindByAdmin(100, 7);
	}

	private static MockHttpSession adminSession() {
		MockHttpSession session = new MockHttpSession();
		session.setAttribute("isAdmin", true);
		AdminCsrf.issue(session);
		return session;
	}

	// ===== 신고 =====

	@Test
	@DisplayName("신고: 첫 신고는 접수, 같은 IP의 재신고는 세지 않고 안내만")
	void report_firstAndDuplicate() throws Exception {
		given(rateLimiterService.tryRecordCommentReport(CLIENT_IP, 7)).willReturn(true, false);

		mockMvc.perform(post(BASE + "/7/report").with(req -> { req.setRemoteAddr(CLIENT_IP); return req; }))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value(containsString("신고가 접수되었습니다")));

		mockMvc.perform(post(BASE + "/7/report").with(req -> { req.setRemoteAddr(CLIENT_IP); return req; }))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("이미 신고한 댓글입니다."));

		verify(commentService).report(100, 7); // 첫 신고 1번만 반영
	}
}
