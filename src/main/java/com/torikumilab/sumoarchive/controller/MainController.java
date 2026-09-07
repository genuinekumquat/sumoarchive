package com.torikumilab.sumoarchive.controller;

import com.torikumilab.sumoarchive.service.BanzukeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class MainController {

	private final BanzukeService banzukeService;

	@GetMapping("/")
	public String main(Model model) {
		// 반즈케 표 자체는 JS가 /api/banzuke로 비동기 로드한다. 여기서는 바쇼 선택 드롭다운 옵션만 내려준다
		// (맨 앞이 최신 바쇼 = 기본 선택).
		model.addAttribute("bashoOptions", banzukeService.listBashoOptions());
		return "index";
	}
}
