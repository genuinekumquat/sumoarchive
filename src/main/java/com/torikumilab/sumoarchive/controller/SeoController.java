package com.torikumilab.sumoarchive.controller;

import com.torikumilab.sumoarchive.domain.entity.constant.Division;
import com.torikumilab.sumoarchive.repository.BanzukeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

/**
 * 검색엔진용 robots.txt / sitemap.xml. 사이트 주소는 설정 대신 요청에서 읽는다
 * (Nginx 뒤에서는 forward-headers-strategy=native 덕분에 https://도메인 으로 나온다).
 */
@RestController
@RequiredArgsConstructor
public class SeoController {

	private final BanzukeRepository banzukeRepository;

	@GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
	public String robots() {
		return """
				User-agent: *
				Disallow: /admin
				Disallow: /api/
				Disallow: /search
				Disallow: /bookmark
				Disallow: /torikumi/*/fragment

				Sitemap: %s/sitemap.xml
				""".formatted(baseUrl());
	}

	/**
	 * 메인(한/일)과 세키토리(마쿠우치·쥬료) 경험이 있는 리키시 프로필(한/일).
	 * 하위 리그만 있는 선수는 대전 기록이 없어 내용이 거의 비어 있으므로 제외.
	 * 언어는 세션 기준이라 ?lang=ja 붙은 주소를 일본어 페이지로 따로 올린다 (쿠키 없는 크롤러는 기본 한국어).
	 */
	@GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
	public String sitemap() {
		String base = baseUrl();
		StringBuilder xml = new StringBuilder("""
				<?xml version="1.0" encoding="UTF-8"?>
				<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
				""");
		url(xml, base + "/");
		url(xml, base + "/?lang=ja");
		for (Integer id : banzukeRepository.findDistinctRikishiIdsByDivisionIn(List.of(Division.Makuuchi, Division.Juryo))) {
			url(xml, base + "/rikishi/" + id);
			url(xml, base + "/rikishi/" + id + "?lang=ja");
		}
		return xml.append("</urlset>\n").toString();
	}

	private static void url(StringBuilder xml, String loc) {
		xml.append("  <url><loc>").append(loc).append("</loc></url>\n");
	}

	private static String baseUrl() {
		return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
	}
}
