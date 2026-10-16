package com.torikumilab.sumoarchive.controller;

import com.torikumilab.sumoarchive.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 관리자 메인 팝업 공지. /admin/** 경로라 AdminAuthInterceptor가 세션 isAdmin 여부를 먼저 검사한다.
 */
@Controller
@RequiredArgsConstructor
public class AdminNoticeViewController {

	private final NoticeService noticeService;

	@GetMapping("/admin/notice")
	public String list(Model model) {
		model.addAttribute("rows", noticeService.list());
		return "admin/notice";
	}

	@PostMapping("/admin/notice")
	public String create(@RequestParam String titleKo,
						 @RequestParam String bodyKo,
						 @RequestParam(required = false) String titleJa,
						 @RequestParam(required = false) String bodyJa,
						 @RequestParam(required = false) String linkUrl,
						 @RequestParam(defaultValue = "false") boolean activate,
						 RedirectAttributes redirectAttributes) {
		try {
			noticeService.create(titleKo, bodyKo, titleJa, bodyJa, linkUrl, activate);
			redirectAttributes.addFlashAttribute("saved", activate ? "공지를 등록하고 메인에 띄웠습니다." : "공지를 등록했습니다 (꺼진 상태).");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "공지 등록 실패: " + e.getMessage());
		}
		return "redirect:/admin/notice";
	}

	@PostMapping("/admin/notice/{id}")
	public String update(@PathVariable Integer id,
						 @RequestParam String titleKo,
						 @RequestParam String bodyKo,
						 @RequestParam(required = false) String titleJa,
						 @RequestParam(required = false) String bodyJa,
						 @RequestParam(required = false) String linkUrl,
						 RedirectAttributes redirectAttributes) {
		try {
			noticeService.update(id, titleKo, bodyKo, titleJa, bodyJa, linkUrl);
			redirectAttributes.addFlashAttribute("saved", "공지를 수정했습니다.");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "공지 수정 실패: " + e.getMessage());
		}
		return "redirect:/admin/notice";
	}

	@PostMapping("/admin/notice/{id}/active")
	public String setActive(@PathVariable Integer id, @RequestParam boolean active,
							RedirectAttributes redirectAttributes) {
		try {
			if (active) {
				noticeService.activate(id);
			} else {
				noticeService.deactivate(id);
			}
			redirectAttributes.addFlashAttribute("saved", active ? "메인에 띄웠습니다." : "공지를 껐습니다.");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "공지 상태 변경 실패: " + e.getMessage());
		}
		return "redirect:/admin/notice";
	}

	@PostMapping("/admin/notice/{id}/delete")
	public String delete(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
		try {
			noticeService.delete(id);
			redirectAttributes.addFlashAttribute("saved", "공지를 삭제했습니다.");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "공지 삭제 실패: " + e.getMessage());
		}
		return "redirect:/admin/notice";
	}
}
