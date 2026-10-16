package com.torikumilab.sumoarchive.controller;

import com.torikumilab.sumoarchive.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 관리자 「의견함」 - 방문자가 /feedback으로 보낸 의견·오류 보고. /admin/** 경로라
 * AdminAuthInterceptor가 세션 isAdmin 여부를 먼저 검사한다.
 */
@Controller
@RequiredArgsConstructor
public class AdminFeedbackViewController {

	private final FeedbackService feedbackService;

	@GetMapping("/admin/feedback")
	public String list(@RequestParam(defaultValue = "false") boolean unhandled,
					   @RequestParam(defaultValue = "0") int page,
					   Model model) {
		model.addAttribute("result", feedbackService.list(unhandled, page));
		model.addAttribute("unhandled", unhandled);
		model.addAttribute("unhandledCount", feedbackService.countUnhandled());
		return "admin/feedback";
	}

	@PostMapping("/admin/feedback/{id}/handled")
	public String setHandled(@PathVariable Integer id, @RequestParam boolean handled,
							 @RequestParam(defaultValue = "false") boolean unhandled,
							 RedirectAttributes redirectAttributes) {
		try {
			feedbackService.setHandled(id, handled);
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "상태 변경 실패: " + e.getMessage());
		}
		return "redirect:/admin/feedback" + (unhandled ? "?unhandled=true" : "");
	}

	@PostMapping("/admin/feedback/{id}/delete")
	public String delete(@PathVariable Integer id,
						 @RequestParam(defaultValue = "false") boolean unhandled,
						 RedirectAttributes redirectAttributes) {
		try {
			feedbackService.delete(id);
			redirectAttributes.addFlashAttribute("saved", "의견을 삭제했습니다.");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "삭제 실패: " + e.getMessage());
		}
		return "redirect:/admin/feedback" + (unhandled ? "?unhandled=true" : "");
	}
}
