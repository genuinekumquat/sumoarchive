package com.torikumilab.sumoarchive.controller;

import com.torikumilab.sumoarchive.service.HeyaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 헤야 페이지 - 메인 "일문" 탭의 헤야 이름, 리키시 프로필의 소속 헤야에서 들어온다.
 */
@Controller
@RequiredArgsConstructor
public class HeyaViewController {

	private final HeyaService heyaService;

	@GetMapping("/heya/{id}")
	public String detail(@PathVariable Integer id, Model model) {
		model.addAttribute("heya", heyaService.getHeyaPage(id));
		return "heya/detail";
	}
}
