package com.torikumilab.sumoarchive.controller;

import com.torikumilab.sumoarchive.service.BanzukeService;
import com.torikumilab.sumoarchive.service.KimariteEncyclopediaService;
import com.torikumilab.sumoarchive.service.NoticeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 메인 첫 탭: 바쇼 기간이면 대진, 아니면 반즈케. 서버가 처음부터 그 탭을 활성으로 그린다(깜빡임 없음).
 */
@WebMvcTest(MainController.class)
class MainControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private BanzukeService banzukeService;
	@MockitoBean
	private KimariteEncyclopediaService kimariteEncyclopediaService;
	@MockitoBean
	private NoticeService noticeService;

	@BeforeEach
	void emptyData() {
		given(banzukeService.listBashoOptions()).willReturn(List.of());
		given(banzukeService.getIchimonStructure()).willReturn(List.of());
		given(kimariteEncyclopediaService.getKimariteEncyclopedia()).willReturn(List.of());
		given(noticeService.findActive(any())).willReturn(Optional.empty());
	}

	@Test
	@DisplayName("바쇼 기간 - 대진 탭이 활성, 반즈케 패널은 숨김")
	void bashoPeriod_opensTorikumi() throws Exception {
		given(banzukeService.isBashoPeriod(any())).willReturn(true);

		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(model().attribute("defaultTab", "torikumi"))
				.andExpect(content().string(containsString("data-default-tab=\"torikumi\"")))
				.andExpect(content().string(containsString("class=\"view-tab active\" id=\"tabTorikumi\"")))
				.andExpect(content().string(containsString("class=\"view-tab\" id=\"tabBanzuke\"")))
				.andExpect(content().string(containsString("id=\"banzukePanel\" hidden=\"hidden\"")))
				.andExpect(content().string(matchesPattern("(?s).*id=\"torikumiPanel\">.*")));
	}

	@Test
	@DisplayName("바쇼 기간 밖 - 지금처럼 반즈케 탭이 활성, 대진 패널은 숨김")
	void offSeason_opensBanzuke() throws Exception {
		given(banzukeService.isBashoPeriod(any())).willReturn(false);

		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(model().attribute("defaultTab", "banzuke"))
				.andExpect(content().string(containsString("class=\"view-tab active\" id=\"tabBanzuke\"")))
				.andExpect(content().string(containsString("class=\"view-tab\" id=\"tabTorikumi\"")))
				.andExpect(content().string(matchesPattern("(?s).*id=\"banzukePanel\">.*")))
				.andExpect(content().string(containsString("id=\"torikumiPanel\" hidden=\"hidden\"")));
	}
}
