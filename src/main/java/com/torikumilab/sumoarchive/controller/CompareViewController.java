package com.torikumilab.sumoarchive.controller;

import com.torikumilab.sumoarchive.service.CompareService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

/**
 * 두 리키시 비교. 둘 다 있으면 비교 결과, 하나만 있으면(프로필의 "다른 선수와 비교") 그 선수를 A에 채운 선택 화면,
 * 없으면 빈 선택 화면. 같은 선수를 두 번 고르면 하나만 고른 것으로 본다.
 */
@Controller
@RequiredArgsConstructor
public class CompareViewController {

	private final CompareService compareService;

	@GetMapping("/compare")
	public String compare(@RequestParam(required = false) Integer a,
						  @RequestParam(required = false) Integer b,
						  Model model) {
		if (a == null) {
			a = b;
			b = null;
		}
		if (a != null && a.equals(b)) {
			b = null;
		}
		if (a != null && b != null) {
			model.addAttribute("compare", compareService.compare(a, b, LocalDate.now()));
		} else if (a != null) {
			model.addAttribute("sideA", compareService.getSide(a));
		}
		model.addAttribute("a", a);
		model.addAttribute("b", b);
		return "compare/index";
	}
}
