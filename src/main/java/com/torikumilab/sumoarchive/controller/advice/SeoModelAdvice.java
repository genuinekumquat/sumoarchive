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

	/**
	 * 현재 페이지의 한국어판 주소 (lang 파라미터만 뺀다). canonical·hreflang용.
	 * 언어는 세션 기준이라 같은 경로가 두 언어로 나오므로, 한국어판 = 경로 그대로 / 일본어판 = ?lang=ja로 고정해 알려준다
	 * (sitemap.xml과 같은 규칙).
	 */
	@ModelAttribute("koUrl")
	public String koUrl() {
		return ServletUriComponentsBuilder.fromCurrentRequest().replaceQueryParam("lang").build().toUriString();
	}

	/** 현재 페이지의 일본어판 주소 (?lang=ja). */
	@ModelAttribute("jaUrl")
	public String jaUrl() {
		return ServletUriComponentsBuilder.fromCurrentRequest().replaceQueryParam("lang", "ja").build().toUriString();
	}

	/** 현재 경로(+쿼리). 푸터 「의견·오류 보내기」 링크가 어느 페이지에서 보냈는지 넘길 때 쓴다. */
	@ModelAttribute("currentPath")
	public String currentPath() {
		return ServletUriComponentsBuilder.fromCurrentRequest().scheme(null).host(null).port(null)
				.build().toUriString();
	}
}
