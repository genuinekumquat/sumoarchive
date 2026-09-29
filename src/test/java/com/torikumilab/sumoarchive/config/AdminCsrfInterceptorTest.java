package com.torikumilab.sumoarchive.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;

class AdminCsrfInterceptorTest {

	private final AdminCsrfInterceptor interceptor = new AdminCsrfInterceptor();

	private static MockHttpServletRequest post(MockHttpSession session) {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin/heya/1");
		request.setSession(session);
		return request;
	}

	@Test
	@DisplayName("GET은 토큰 없이 통과")
	void getPasses() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/heya");
		assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), null)).isTrue();
	}

	@Test
	@DisplayName("POST - 폼 필드(_csrf) 또는 헤더(X-CSRF-TOKEN)가 세션 토큰과 같으면 통과")
	void validTokenPasses() throws Exception {
		MockHttpSession session = new MockHttpSession();
		String token = AdminCsrf.issue(session);

		MockHttpServletRequest byParam = post(session);
		byParam.addParameter(AdminCsrf.PARAM, token);
		assertThat(interceptor.preHandle(byParam, new MockHttpServletResponse(), null)).isTrue();

		MockHttpServletRequest byHeader = post(session);
		byHeader.addHeader(AdminCsrf.HEADER, token);
		assertThat(interceptor.preHandle(byHeader, new MockHttpServletResponse(), null)).isTrue();
	}

	@Test
	@DisplayName("POST - 토큰이 없거나 다르거나, 세션에 토큰이 없으면 403")
	void invalidTokenRejected() throws Exception {
		MockHttpSession session = new MockHttpSession();
		AdminCsrf.issue(session);

		MockHttpServletResponse missing = new MockHttpServletResponse();
		assertThat(interceptor.preHandle(post(session), missing, null)).isFalse();
		assertThat(missing.getStatus()).isEqualTo(403);

		MockHttpServletRequest wrong = post(session);
		wrong.addParameter(AdminCsrf.PARAM, "forged");
		MockHttpServletResponse wrongRes = new MockHttpServletResponse();
		assertThat(interceptor.preHandle(wrong, wrongRes, null)).isFalse();
		assertThat(wrongRes.getStatus()).isEqualTo(403);

		MockHttpServletRequest noSessionToken = post(new MockHttpSession());
		noSessionToken.addParameter(AdminCsrf.PARAM, "anything");
		assertThat(interceptor.preHandle(noSessionToken, new MockHttpServletResponse(), null)).isFalse();
	}

	@Test
	@DisplayName("토큰은 로그인마다 새로 발급되고, 숨김 필드 훅은 토큰이 있을 때만 _csrf를 붙인다")
	void issueAndHiddenField() {
		MockHttpSession session = new MockHttpSession();
		String first = AdminCsrf.issue(session);
		String second = AdminCsrf.issue(session);
		assertThat(first).isNotEqualTo(second).hasSizeGreaterThanOrEqualTo(40);

		CsrfRequestDataValueProcessor processor = new CsrfRequestDataValueProcessor();
		MockHttpServletRequest withToken = new MockHttpServletRequest();
		withToken.setSession(session);
		assertThat(processor.getExtraHiddenFields(withToken)).containsEntry(AdminCsrf.PARAM, second);
		assertThat(processor.getExtraHiddenFields(new MockHttpServletRequest())).isEmpty();
	}
}
