package com.torikumilab.sumoarchive.controller;

import com.torikumilab.sumoarchive.domain.dto.RikishiAdminRowDTO;
import com.torikumilab.sumoarchive.domain.dto.RikishiEditFormDTO;
import com.torikumilab.sumoarchive.domain.entity.constant.RankName;
import com.torikumilab.sumoarchive.service.RikishiAdminService;
import com.torikumilab.sumoarchive.service.RosterImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 관리자 "데이터 수동 갱신" 중 리키시 프로필 수정. /admin/** 경로라 AdminAuthInterceptor가
 * 세션 isAdmin 여부를 먼저 검사한다.
 */
@Controller
@RequiredArgsConstructor
public class AdminRikishiViewController {

	private static final int PAGE_SIZE = 30;

	private final RikishiAdminService rikishiAdminService;
	private final RosterImportService rosterImportService;

	// 소속 헤야 select의 "무소속"(빈 문자열) 옵션을 null로 변환.
	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.registerCustomEditor(Integer.class, "heyaId", new CustomNumberEditor(Integer.class, true));
	}

	@GetMapping("/admin/rikishi")
	public String list(@RequestParam(required = false) String keyword,
						@RequestParam(defaultValue = "0") int page,
						Model model) {
		Page<RikishiAdminRowDTO> result = rikishiAdminService.list(
				keyword, PageRequest.of(page, PAGE_SIZE, Sort.by("id")));
		model.addAttribute("result", result);
		model.addAttribute("keyword", keyword);
		model.addAttribute("missingKrCount", rikishiAdminService.countMissingKoreanShikona());
		return "admin/rikishi/list";
	}

	/** 한국어 시코나가 없는 리키시에 로마자 음차로 1차값을 채운다 ("자동" 표시 켠 채). */
	@PostMapping("/admin/rikishi/shikona/autofill")
	public String autofillShikona(RedirectAttributes redirectAttributes) {
		try {
			redirectAttributes.addFlashAttribute("autofillResult",
					rikishiAdminService.autofillKoreanShikona());
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "한국어 시코나 자동 채우기 실패: " + e.getMessage());
		}
		return "redirect:/admin/rikishi";
	}

	@GetMapping("/admin/rikishi/{id}/edit")
	public String editForm(@PathVariable Integer id, Model model) {
		model.addAttribute("form", rikishiAdminService.getEditForm(id));
		model.addAttribute("heyaOptions", rikishiAdminService.getHeyaOptions());
		model.addAttribute("rankOptions", RankName.values());
		return "admin/rikishi/edit";
	}

	@PostMapping("/admin/rikishi/{id}/edit")
	public String update(@PathVariable Integer id,
						  @ModelAttribute("form") RikishiEditFormDTO form,
						  RedirectAttributes redirectAttributes) {
		rikishiAdminService.updateProfile(id, form);
		redirectAttributes.addFlashAttribute("saved", true);
		return "redirect:/admin/rikishi/" + id + "/edit";
	}

	/**
	 * sumo-api에서 로스터(헤야 + 현역 리키시)를 통째로 가져와 재구성한다.
	 * 기존 리키시·헤야·하위 데이터는 전부 삭제되므로 화면에서 confirm을 받는다.
	 */
	@PostMapping("/admin/rikishi/import")
	public String importRoster(RedirectAttributes redirectAttributes) {
		try {
			redirectAttributes.addFlashAttribute("importResult", rosterImportService.importRoster());
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "sumo-api 임포트 실패: " + e.getMessage());
		}
		return "redirect:/admin/rikishi";
	}
}
