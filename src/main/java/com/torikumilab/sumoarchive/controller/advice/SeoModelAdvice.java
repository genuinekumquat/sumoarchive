package com.torikumilab.sumoarchive.controller.advice;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * 화면 공통 head 조각(fragments/common :: seo)이 쓰는 절대 주소.
 * og:url·og:image는 절대 주소여야 카카오톡·X 등에서 미리보기가 뜬다.
 */
@ControllerAdvice(annotations = Controller.class)
public class SeoModelAdvice {

	@ModelAttribute("baseUrl")
	public String baseUrl() {
		return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
	}

	@ModelAttribute("currentUrl")
	public String currentUrl() {
		return ServletUriComponentsBuilder.fromCurrentRequest().build().toUriString();
	}
}
