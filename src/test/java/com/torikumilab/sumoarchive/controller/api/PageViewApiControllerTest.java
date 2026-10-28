package com.torikumilab.sumoarchive.controller.api;

import com.torikumilab.sumoarchive.repository.RikishiRepository;
import com.torikumilab.sumoarchive.repository.TorikumiRepository;
import com.torikumilab.sumoarchive.service.stats.VisitClassifier.Page;
import com.torikumilab.sumoarchive.service.stats.VisitClassifier.PageType;
import com.torikumilab.sumoarchive.service.stats.VisitStatsCollector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 방문 통계 beacon: 사람 브라우저의 공개 페이지만 세고, 봇·관리자·통계 밖 경로·없는 id는 버린다. 응답은 항상 204.
 */
@WebMvcTest(PageViewApiController.class)
class PageViewApiControllerTest {

	private static final String CHROME = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private VisitStatsCollector visitStatsCollector;
	@MockitoBean
	private RikishiRepository rikishiRepository;
	@MockitoBean
	private TorikumiRepository torikumiRepository;

	private static MockHttpServletRequestBuilder beacon(String path, String ref) {
		return post("/api/pv").param("path", path).param("ref", ref)
				.header(HttpHeaders.USER_AGENT, CHROME).header(HttpHeaders.ACCEPT_LANGUAGE, "ko-KR,ko;q=0.9");
	}

	@Test
	@DisplayName("있는 리키시 페이지는 외부 유입처와 함께 센다")
	void rikishi_recorded() throws Exception {
		given(rikishiRepository.existsById(1257)).willReturn(true);

		mockMvc.perform(beacon("/rikishi/1257", "https://www.google.co.kr/")).andExpect(status().isNoContent());

		verify(visitStatsCollector).record(eq(new Page(PageType.RIKISHI, 1257)), anyString(), eq(CHROME), eq("google"));
	}

	@Test
	@DisplayName("메인은 id 확인 없이 세고, 빈 referrer는 유입처 없음")
	void main_withoutReferrer() throws Exception {
		mockMvc.perform(beacon("/", "")).andExpect(status().isNoContent());

		verify(visitStatsCollector).record(eq(new Page(PageType.MAIN, 0)), anyString(), eq(CHROME), isNull());
	}

	@Test
	@DisplayName("없는 리키시·경기 id는 세지 않는다")
	void unknownId_ignored() throws Exception {
		mockMvc.perform(beacon("/rikishi/99999", "")).andExpect(status().isNoContent());
		mockMvc.perform(beacon("/torikumi/99999", "")).andExpect(status().isNoContent());

		verify(visitStatsCollector, never()).record(any(), any(), any(), any());
	}

	@Test
	@DisplayName("통계 밖 경로·봇·관리자는 세지 않는다")
	void ignored() throws Exception {
		mockMvc.perform(beacon("/admin/stats", "")).andExpect(status().isNoContent());
		mockMvc.perform(post("/api/pv").param("path", "/")
						.header(HttpHeaders.USER_AGENT, "Mozilla/5.0 (compatible; Googlebot/2.1)").header(HttpHeaders.ACCEPT_LANGUAGE, "en"))
				.andExpect(status().isNoContent());
		mockMvc.perform(post("/api/pv").param("path", "/").header(HttpHeaders.USER_AGENT, CHROME)) // Accept-Language 없음
				.andExpect(status().isNoContent());
		MockHttpSession admin = new MockHttpSession();
		admin.setAttribute("isAdmin", true);
		mockMvc.perform(beacon("/", "").session(admin)).andExpect(status().isNoContent());

		verify(visitStatsCollector, never()).record(any(), any(), any(), any());
	}
}
