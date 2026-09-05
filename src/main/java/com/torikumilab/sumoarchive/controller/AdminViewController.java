package com.torikumilab.sumoarchive.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 기능명세서 [사이트 관리자] 절: 별도 메뉴 노출 없이 /admin/login 주소 직접 입력으로만 접근.
 * Spring Security 없이 세션 isAdmin=true 여부로만 제어하는 경량 구현이며,
 * /admin/** 하위 화면 보호는 AdminAuthInterceptor(WebConfig에 등록)가 담당한다.
 */
@Controller
public class AdminViewController {

	@Value("${admin.username}")
	private String adminUsername;

	@Value("${admin.password}")
	private String adminPassword;

	@GetMapping("/admin/login")
	public String loginForm(HttpSession session, Model model) {
		if (Boolean.TRUE.equals(session.getAttribute("isAdmin"))) {
			model.addAttribute("alreadyLoggedIn", true);
		}
		return "admin/login";
	}

	@PostMapping("/admin/login")
	public String login(@RequestParam String username, @RequestParam String password,
						 HttpSession session, Model model) {
		if (adminUsername.equals(username) && adminPassword.equals(password)) {
			session.setAttribute("isAdmin", true);
			return "redirect:/admin/comments";
		}
		model.addAttribute("error", "아이디 또는 비밀번호가 올바르지 않습니다.");
		return "admin/login";
	}

	@PostMapping("/admin/logout")
	public String logout(HttpSession session) {
		session.invalidate();
		return "redirect:/admin/login";
	}
}
