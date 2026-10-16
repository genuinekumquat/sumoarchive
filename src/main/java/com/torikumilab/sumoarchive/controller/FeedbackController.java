package com.torikumilab.sumoarchive.controller;

import com.torikumilab.sumoarchive.domain.entity.constant.FeedbackCategory;
import com.torikumilab.sumoarchive.service.FeedbackService;
import com.torikumilab.sumoarchive.service.exception.LocalizedMessage;
import com.torikumilab.sumoarchive.service.exception.RateLimitExceededException;
import com.torikumilab.sumoarchive.util.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Locale;

/**
 * 의견·오류 보내기. 모든 공개 페이지 푸터(fragments/common :: footerNote)에서 ?from=현재 경로로 연결한다.
 * 보낸 내용은 공개되지 않고 관리자 「의견함」에서만 본다. JS 없이 동작하는 일반 폼(PRG).
 */
@Controller
@RequiredArgsConstructor
public class FeedbackController {

	private final FeedbackService feedbackService;
	private final MessageSource messageSource;

	@GetMapping("/feedback")
	public String form(@RequestParam(required = false) String from, Model model) {
		model.addAttribute("from", FeedbackService.pagePath(from));
		model.addAttribute("category", FeedbackCategory.BUG.name());
		return form(model);
	}

	@PostMapping("/feedback")
	public String submit(@RequestParam(required = false) String category,
						 @RequestParam(required = false) String content,
						 @RequestParam(required = false) String contact,
						 @RequestParam(required = false) String from,
						 @RequestParam(required = false) String website,
						 HttpServletRequest request, Locale locale, Model model,
						 RedirectAttributes redirectAttributes) {
		// website는 화면에 안 보이는 칸이라 사람은 비워 둔다. 채워져 있으면 봇이므로 저장하지 않고 보낸 척만 한다.
		if (website != null && !website.isBlank()) {
			redirectAttributes.addFlashAttribute("sent", true);
			return "redirect:/feedback";
		}
		try {
			feedbackService.submit(category, content, contact, from, locale, ClientIpResolver.getClientIp(request));
		} catch (IllegalArgumentException | RateLimitExceededException e) {
			// 입력값을 그대로 돌려줘서 다시 쓰지 않아도 되게 한다
			Object[] args = e instanceof LocalizedMessage lm ? lm.getMessageArgs() : null;
			model.addAttribute("error", messageSource.getMessage(e.getMessage(), args, e.getMessage(), locale));
			model.addAttribute("from", FeedbackService.pagePath(from));
			model.addAttribute("category", FeedbackService.parseCategory(category).name());
			model.addAttribute("content", content);
			model.addAttribute("contact", contact);
			return form(model);
		}
		redirectAttributes.addFlashAttribute("sent", true);
		return "redirect:/feedback";
	}

	private String form(Model model) {
		model.addAttribute("categories", FeedbackCategory.values());
		model.addAttribute("contentMax", FeedbackService.CONTENT_MAX);
		model.addAttribute("contactMax", FeedbackService.CONTACT_MAX);
		return "feedback";
	}
}
