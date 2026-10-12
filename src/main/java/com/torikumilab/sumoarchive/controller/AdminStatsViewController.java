package com.torikumilab.sumoarchive.controller;

import com.torikumilab.sumoarchive.service.stats.VisitStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 관리자 방문 통계. /admin/** 경로라 AdminAuthInterceptor(WebConfig)가 세션 isAdmin 여부를 먼저 검사한다.
 * 숫자는 VisitStatsCollector가 1분마다 쌓는 날짜별 합계 (봇·관리자 로그인 상태 제외, IP 미저장).
 */
@Controller
@RequiredArgsConstructor
public class AdminStatsViewController {

	private final VisitStatsService visitStatsService;

	@GetMapping("/admin/stats")
	public String stats(@RequestParam(defaultValue = "30") int days, Model model) {
		model.addAttribute("s", visitStatsService.summary(days));
		model.addAttribute("periods", VisitStatsService.PERIODS);
		return "admin/stats";
	}
}
