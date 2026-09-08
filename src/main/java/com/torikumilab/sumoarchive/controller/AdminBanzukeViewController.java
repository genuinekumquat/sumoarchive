package com.torikumilab.sumoarchive.controller;

import com.torikumilab.sumoarchive.domain.dto.BanzukeFormDTO;
import com.torikumilab.sumoarchive.domain.dto.BashoCreateFormDTO;
import com.torikumilab.sumoarchive.domain.dto.BashoEditFormDTO;
import com.torikumilab.sumoarchive.domain.entity.constant.BashoMonth;
import com.torikumilab.sumoarchive.domain.entity.constant.Division;
import com.torikumilab.sumoarchive.domain.entity.constant.Side;
import com.torikumilab.sumoarchive.service.AwardImportService;
import com.torikumilab.sumoarchive.service.BanzukeAdminService;
import com.torikumilab.sumoarchive.service.BanzukeImportService;
import com.torikumilab.sumoarchive.service.BashoImportService;
import com.torikumilab.sumoarchive.util.RankDisplayUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 관리자 "데이터 수동 갱신" 중 반즈케(番付) 관리. /admin/** 경로라 AdminAuthInterceptor(WebConfig)가
 * 세션 isAdmin 여부를 먼저 검사한다.
 *
 * <p>변경(POST)은 성공/검증실패 모두 목록 화면으로 redirect + flash 메시지로 통일한다.
 * 존재하지 않는 바쇼/행은 EntityNotFoundException을 던져 ViewExceptionHandler가 404로 연결한다.</p>
 */
@Controller
@RequiredArgsConstructor
public class AdminBanzukeViewController {

	private final BanzukeAdminService banzukeAdminService;
	private final BashoImportService bashoImportService;
	private final BanzukeImportService banzukeImportService;
	private final AwardImportService awardImportService;

	// 빈 <input type="number">("")를 null로 (요코즈나 등 번호 없는 계급). AdminRikishiViewController와 동일 기법.
	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.registerCustomEditor(Integer.class, "rankValue", new CustomNumberEditor(Integer.class, true));
	}

	// ===== 바쇼 =====

	@GetMapping("/admin/basho")
	public String bashoList(Model model) {
		model.addAttribute("bashos", banzukeAdminService.listBashos());
		model.addAttribute("thisYear", java.time.LocalDate.now().getYear());
		return "admin/basho/list";
	}

	/** sumo-api에서 연도 범위의 바쇼 메타를 가져온다 (본장소 6개월 × 연도). */
	@PostMapping("/admin/basho/import")
	public String importBashos(@RequestParam(required = false) Integer fromYear,
							   @RequestParam(required = false) Integer toYear,
							   RedirectAttributes redirectAttributes) {
		try {
			redirectAttributes.addFlashAttribute("importResult", bashoImportService.importRange(fromYear, toYear));
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "sumo-api 바쇼 임포트 실패: " + e.getMessage());
		}
		return "redirect:/admin/basho";
	}

	/** 한 바쇼의 우승·삼상(sumo-api) + 킨보시(반즈케·토리쿠미 파생)를 채운다. 재실행 가능(빠진 것만 추가). */
	@PostMapping("/admin/basho/{bashoId}/awards/import")
	public String importAwards(@PathVariable Integer bashoId, RedirectAttributes redirectAttributes) {
		try {
			redirectAttributes.addFlashAttribute("awardResult", awardImportService.importForBasho(bashoId));
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "우승·삼상·킨보시 갱신 실패: " + e.getMessage());
		}
		return "redirect:/admin/basho";
	}

	@GetMapping("/admin/basho/new")
	public String bashoForm(Model model) {
		if (!model.containsAttribute("form")) {
			model.addAttribute("form", new BashoCreateFormDTO());
		}
		model.addAttribute("monthOptions", BashoMonth.values());
		return "admin/basho/form";
	}

	@PostMapping("/admin/basho")
	public String createBasho(@ModelAttribute("form") BashoCreateFormDTO form,
							  RedirectAttributes redirectAttributes) {
		try {
			banzukeAdminService.createBasho(form);
			redirectAttributes.addFlashAttribute("saved", true);
			return "redirect:/admin/basho";
		} catch (IllegalArgumentException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			redirectAttributes.addFlashAttribute("form", form);
			return "redirect:/admin/basho/new";
		}
	}

	@GetMapping("/admin/basho/{bashoId}/edit")
	public String bashoEditForm(@PathVariable Integer bashoId, Model model) {
		if (!model.containsAttribute("form")) {
			model.addAttribute("form", banzukeAdminService.getBashoEditForm(bashoId));
		}
		return "admin/basho/edit";
	}

	@PostMapping("/admin/basho/{bashoId}")
	public String updateBasho(@PathVariable Integer bashoId,
							  @ModelAttribute("form") BashoEditFormDTO form,
							  RedirectAttributes redirectAttributes) {
		try {
			banzukeAdminService.updateBasho(bashoId, form);
			redirectAttributes.addFlashAttribute("saved", true);
			return "redirect:/admin/basho";
		} catch (IllegalArgumentException e) {
			// 폼은 다시 DB에서 로드한다(연/월/라벨은 화면 표시 전용이라 POST에 실려오지 않음).
			// 날짜 2칸만 다시 입력하면 되므로 입력값 유지를 위한 별도 처리는 생략.
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/admin/basho/" + bashoId + "/edit";
		}
	}

	@PostMapping("/admin/basho/{bashoId}/delete")
	public String deleteBasho(@PathVariable Integer bashoId, RedirectAttributes redirectAttributes) {
		try {
			banzukeAdminService.deleteBasho(bashoId);
			redirectAttributes.addFlashAttribute("saved", true);
			return "redirect:/admin/basho";
		} catch (IllegalArgumentException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/admin/basho/" + bashoId + "/edit";
		}
	}

	// ===== 반즈케 행 =====

	@GetMapping("/admin/basho/{bashoId}/banzuke")
	public String banzukeBoard(@PathVariable Integer bashoId,
							   @RequestParam(defaultValue = "Makuuchi") Division division,
							   Model model) {
		model.addAttribute("board", banzukeAdminService.getBoard(bashoId, division));
		model.addAttribute("rikishiOptions", banzukeAdminService.getRikishiOptions());
		model.addAttribute("divisionOptions", Division.values());
		model.addAttribute("rankOptions", RankDisplayUtil.koreanLabelOptions());
		model.addAttribute("sideOptions", Side.values());
		model.addAttribute("division", division);
		return "admin/basho/banzuke";
	}

	/** sumo-api에서 이 바쇼·디비전의 반즈케를 가져온다 (externalApiId로 리키시 매칭, 미매칭은 스킵). */
	@PostMapping("/admin/basho/{bashoId}/banzuke/import")
	public String importBanzuke(@PathVariable Integer bashoId,
								@RequestParam(defaultValue = "Makuuchi") Division division,
								RedirectAttributes redirectAttributes) {
		try {
			redirectAttributes.addFlashAttribute("importResult",
					banzukeImportService.importDivision(bashoId, division));
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "sumo-api 반즈케 임포트 실패: " + e.getMessage());
		}
		return redirectToBoard(bashoId, division);
	}

	@PostMapping("/admin/basho/{bashoId}/banzuke")
	public String addRow(@PathVariable Integer bashoId,
						 @ModelAttribute BanzukeFormDTO form,
						 RedirectAttributes redirectAttributes) {
		try {
			banzukeAdminService.addRow(bashoId, form);
			redirectAttributes.addFlashAttribute("saved", true);
		} catch (IllegalArgumentException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
		}
		return redirectToBoard(bashoId, form.getDivision());
	}

	@PostMapping("/admin/basho/{bashoId}/banzuke/{banzukeId}")
	public String updateRow(@PathVariable Integer bashoId,
							@PathVariable Integer banzukeId,
							@ModelAttribute BanzukeFormDTO form,
							RedirectAttributes redirectAttributes) {
		try {
			banzukeAdminService.updateRow(bashoId, banzukeId, form);
			redirectAttributes.addFlashAttribute("saved", true);
		} catch (IllegalArgumentException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
		}
		return redirectToBoard(bashoId, form.getDivision());
	}

	@PostMapping("/admin/basho/{bashoId}/banzuke/{banzukeId}/delete")
	public String deleteRow(@PathVariable Integer bashoId,
							@PathVariable Integer banzukeId,
							@RequestParam(defaultValue = "Makuuchi") Division division,
							RedirectAttributes redirectAttributes) {
		banzukeAdminService.deleteRow(bashoId, banzukeId);
		redirectAttributes.addFlashAttribute("saved", true);
		return redirectToBoard(bashoId, division);
	}

	private String redirectToBoard(Integer bashoId, Division division) {
		Division d = (division != null) ? division : Division.Makuuchi;
		return "redirect:/admin/basho/" + bashoId + "/banzuke?division=" + d.name();
	}
}
