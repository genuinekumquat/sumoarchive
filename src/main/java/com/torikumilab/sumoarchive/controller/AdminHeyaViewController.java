package com.torikumilab.sumoarchive.controller;

import com.torikumilab.sumoarchive.domain.dto.HeyaAdminRowDTO;
import com.torikumilab.sumoarchive.service.HeyaAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

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
		List<HeyaAdminRowDTO> rows = heyaAdminService.list();
		model.addAttribute("rows", rows);
		model.addAttribute("attentionRows", rows.stream().filter(HeyaAdminRowDTO::needsAttention).toList());
		model.addAttribute("ichimonOptions", heyaAdminService.ichimonOptions());
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

	/** 헤야 합치기(개명·흡수) - 소속 선수를 target으로 옮기고, 이후 임포트도 target으로. */
	@PostMapping("/admin/heya/{id}/merge")
	public String merge(@PathVariable Integer id,
						@RequestParam Integer targetId,
						RedirectAttributes redirectAttributes) {
		try {
			int moved = heyaAdminService.merge(id, targetId);
			redirectAttributes.addFlashAttribute("mergeMessage", "헤야를 합쳤습니다 — 리키시 " + moved + "명 이동");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "헤야 합치기 실패: " + e.getMessage());
		}
		return "redirect:/admin/heya";
	}

	@PostMapping("/admin/heya/{id}/unmerge")
	public String unmerge(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
		try {
			heyaAdminService.unmerge(id);
			redirectAttributes.addFlashAttribute("mergeMessage", "합치기를 해제했습니다 (옮긴 리키시는 그대로)");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "합치기 해제 실패: " + e.getMessage());
		}
		return "redirect:/admin/heya";
	}

	@PostMapping("/admin/heya/{id}")
	public String save(@PathVariable Integer id,
					   @RequestParam String nameKr,
					   @RequestParam String nameJp,
					   @RequestParam(required = false) String ichimonKr,
					   RedirectAttributes redirectAttributes) {
		try {
			heyaAdminService.saveNames(id, nameKr, nameJp, ichimonKr);
			redirectAttributes.addFlashAttribute("saved", true);
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "헤야명 저장 실패: " + e.getMessage());
		}
		return "redirect:/admin/heya";
	}
}
