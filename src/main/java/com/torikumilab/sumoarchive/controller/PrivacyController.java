package com.torikumilab.sumoarchive.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 개인정보 처리 안내 페이지. 모든 공개 페이지 푸터(fragments/common :: footerNote)에서 연결한다.
 * 문의 이메일은 설정값(app.contact-email / 운영은 CONTACT_EMAIL 환경변수 필수)에서 받는다.
 */
@Controller
public class PrivacyController {

	private final String contactEmail;

	public PrivacyController(@Value("${app.contact-email}") String contactEmail) {
		this.contactEmail = contactEmail;
	}

	@GetMapping("/privacy")
	public String privacy(Model model) {
		model.addAttribute("contactEmail", contactEmail == null || contactEmail.isBlank() ? null : contactEmail.strip());
		return "privacy";
	}
}
