package com.torikumilab.sumoarchive.controller;

import com.torikumilab.sumoarchive.service.HeyaAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 관리자 "데이터 수동 갱신" 중 헤야 한국어명 보강. /admin/** 경로라 AdminAuthInterceptor가
 * 세션 isAdmin 여부를 먼저 검사한다.
 */
@Controller
@RequiredArgsConstructor
public class AdminHeyaViewController {

	private final HeyaAdminService heyaAdminService;

	@GetMapping("/admin/heya")
	public String list(Model model) {
		model.addAttribute("rows", heyaAdminService.list());
		model.addAttribute("missingKrCount", heyaAdminService.countMissingKoreanName());
		return "admin/heya/list";
	}

	/** 한국어명이 없거나 자동 음차값인 헤야에 로마자 음차로 1차값을 (재)생성한다. */
	@PostMapping("/admin/heya/autofill")
	public String autofill(RedirectAttributes redirectAttributes) {
		try {
			redirectAttributes.addFlashAttribute("autofillResult", heyaAdminService.autofillKoreanName());
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "한국어 헤야명 자동 채우기 실패: " + e.getMessage());
		}
		return "redirect:/admin/heya";
	}

	@PostMapping("/admin/heya/{id}")
	public String save(@PathVariable Integer id,
					   @RequestParam String nameKr,
					   @RequestParam String nameJp,
					   RedirectAttributes redirectAttributes) {
		try {
			heyaAdminService.saveNames(id, nameKr, nameJp);
			redirectAttributes.addFlashAttribute("saved", true);
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "헤야명 저장 실패: " + e.getMessage());
		}
		return "redirect:/admin/heya";
	}
}
