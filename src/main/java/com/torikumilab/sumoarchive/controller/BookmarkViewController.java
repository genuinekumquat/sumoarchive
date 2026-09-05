package com.torikumilab.sumoarchive.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 북마크(즐겨찾기) 페이지. LocalStorage에 저장된 ID 배열을 서버는 전혀 모르므로,
 * 이 컨트롤러는 빈 화면 뼈대만 내려주고 실제 목록 조회는 static/js/bookmark.js가
 * /api/rikishiEntity?ids=... 를 fetch해서 채운다.
 */
@Controller
public class BookmarkViewController {

	@GetMapping("/bookmark")
	public String bookmark() {
		return "bookmark/list";
	}
}
