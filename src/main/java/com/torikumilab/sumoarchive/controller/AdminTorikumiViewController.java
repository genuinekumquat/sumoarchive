package com.torikumilab.sumoarchive.controller;

import com.torikumilab.sumoarchive.domain.dto.TorikumiFormDTO;
import com.torikumilab.sumoarchive.domain.entity.constant.Division;
import com.torikumilab.sumoarchive.domain.entity.constant.ResultType;
import com.torikumilab.sumoarchive.domain.entity.constant.Side;
import com.torikumilab.sumoarchive.service.TorikumiAdminService;
import com.torikumilab.sumoarchive.service.TorikumiImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 관리자 "데이터 수동 갱신" 중 토리쿠미(取組, 대전) 관리. /admin/** 경로라 AdminAuthInterceptor가
 * 세션 isAdmin 여부를 먼저 검사한다.
 *
 * <p>필드가 많아 반즈케 관리(인라인 편집)와 달리 리키시 프로필 수정처럼 "목록 + 별도 편집 폼" 구조.
 * POST는 성공/검증실패 모두 redirect + flash로 통일. 없는 바쇼/대전은 EntityNotFoundException →
 * ViewExceptionHandler가 404로 연결.</p>
 */
@Controller
@RequiredArgsConstructor
public class AdminTorikumiViewController {

	private final TorikumiAdminService torikumiAdminService;
	private final TorikumiImportService torikumiImportService;

	// <select>/<input type=number>의 빈 값("")을 null로 (day, eastRikishiId, westRikishiId 공용).
	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.registerCustomEditor(Integer.class, new CustomNumberEditor(Integer.class, true));
	}

	@GetMapping("/admin/basho/{bashoId}/torikumi")
	public String board(@PathVariable Integer bashoId,
						@RequestParam(defaultValue = "1") int day,
						@RequestParam(defaultValue = "Makuuchi") Division division,
						Model model) {
		model.addAttribute("board", torikumiAdminService.getBoard(bashoId, day, division));
		model.addAttribute("day", day);
		model.addAttribute("division", division);
		model.addAttribute("divisionOptions", Division.values());
		model.addAttribute("days", java.util.stream.IntStream.rangeClosed(1, 15).boxed().toList());
		return "admin/basho/torikumi-list";
	}

	/** sumo-api에서 이 바쇼·디비전의 15일치 대전을 가져온다 (externalId upsert, 미매칭 리키시는 스킵). */
	@PostMapping("/admin/basho/{bashoId}/torikumi/import")
	public String importTorikumi(@PathVariable Integer bashoId,
								 @RequestParam(defaultValue = "1") int day,
								 @RequestParam(defaultValue = "Makuuchi") Division division,
								 RedirectAttributes ra) {
		try {
			ra.addFlashAttribute("importResult", torikumiImportService.importDivision(bashoId, division));
		} catch (Exception e) {
			ra.addFlashAttribute("error", "sumo-api 토리쿠미 임포트 실패: " + e.getMessage());
		}
		return redirectToBoard(bashoId, day, division);
	}

	@GetMapping("/admin/basho/{bashoId}/torikumi/new")
	public String newForm(@PathVariable Integer bashoId,
						  @RequestParam(defaultValue = "1") int day,
						  @RequestParam(defaultValue = "Makuuchi") Division division,
						  Model model) {
		if (!model.containsAttribute("form")) {
			TorikumiFormDTO form = new TorikumiFormDTO();
			form.setDay(day);
			form.setDivision(division);
			form.setResultType(ResultType.NORMAL);
			form.setWinnerSide("");
			model.addAttribute("form", form);
		}
		populateFormRefs(bashoId, model);
		model.addAttribute("mode", "new");
		return "admin/basho/torikumi-form";
	}

	@PostMapping("/admin/basho/{bashoId}/torikumi")
	public String create(@PathVariable Integer bashoId,
						 @ModelAttribute("form") TorikumiFormDTO form,
						 RedirectAttributes ra) {
		try {
			torikumiAdminService.addMatch(bashoId, form);
			ra.addFlashAttribute("saved", true);
			return redirectToBoard(bashoId, form.getDay(), form.getDivision());
		} catch (IllegalArgumentException e) {
			ra.addFlashAttribute("error", e.getMessage());
			ra.addFlashAttribute("form", form);
			return "redirect:/admin/basho/" + bashoId + "/torikumi/new";
		}
	}

	@GetMapping("/admin/basho/{bashoId}/torikumi/{torikumiId}/edit")
	public String editForm(@PathVariable Integer bashoId,
						   @PathVariable Integer torikumiId,
						   Model model) {
		if (!model.containsAttribute("form")) {
			model.addAttribute("form", torikumiAdminService.getForm(bashoId, torikumiId));
		}
		populateFormRefs(bashoId, model);
		model.addAttribute("mode", "edit");
		model.addAttribute("torikumiId", torikumiId);
		return "admin/basho/torikumi-form";
	}

	@PostMapping("/admin/basho/{bashoId}/torikumi/{torikumiId}")
	public String update(@PathVariable Integer bashoId,
						 @PathVariable Integer torikumiId,
						 @ModelAttribute("form") TorikumiFormDTO form,
						 RedirectAttributes ra) {
		try {
			torikumiAdminService.updateMatch(bashoId, torikumiId, form);
			ra.addFlashAttribute("saved", true);
			return redirectToBoard(bashoId, form.getDay(), form.getDivision());
		} catch (IllegalArgumentException e) {
			ra.addFlashAttribute("error", e.getMessage());
			ra.addFlashAttribute("form", form);
			return "redirect:/admin/basho/" + bashoId + "/torikumi/" + torikumiId + "/edit";
		}
	}

	@PostMapping("/admin/basho/{bashoId}/torikumi/{torikumiId}/delete")
	public String delete(@PathVariable Integer bashoId,
						 @PathVariable Integer torikumiId,
						 @RequestParam(defaultValue = "1") int day,
						 @RequestParam(defaultValue = "Makuuchi") Division division,
						 RedirectAttributes ra) {
		torikumiAdminService.deleteMatch(bashoId, torikumiId);
		ra.addFlashAttribute("saved", true);
		return redirectToBoard(bashoId, day, division);
	}

	private void populateFormRefs(Integer bashoId, Model model) {
		model.addAttribute("bashoId", bashoId);
		model.addAttribute("rikishiOptions", torikumiAdminService.getRikishiOptions());
		model.addAttribute("divisionOptions", Division.values());
		model.addAttribute("sideOptions", Side.values());
		model.addAttribute("resultTypeOptions", ResultType.values());
		model.addAttribute("kimariteSuggestions", torikumiAdminService.getKimariteSuggestions());
	}

	private String redirectToBoard(Integer bashoId, Integer day, Division division) {
		int d = (day != null && day >= 1) ? day : 1;
		Division dv = (division != null) ? division : Division.Makuuchi;
		return "redirect:/admin/basho/" + bashoId + "/torikumi?day=" + d + "&division=" + dv.name();
	}
}
