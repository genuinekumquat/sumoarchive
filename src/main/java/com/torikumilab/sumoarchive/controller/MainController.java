package com.torikumilab.sumoarchive.controller;

import com.torikumilab.sumoarchive.service.BanzukeService;
import com.torikumilab.sumoarchive.service.KimariteEncyclopediaService;
import com.torikumilab.sumoarchive.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;

@Controller
@RequiredArgsConstructor
public class MainController {

	// 바쇼 일정·대진 발표는 일본 날짜 기준 (자동 임포트와 같은 시간대)
	private static final ZoneId BASHO_ZONE = ZoneId.of("Asia/Tokyo");

	private final BanzukeService banzukeService;
	private final KimariteEncyclopediaService kimariteEncyclopediaService;
	private final NoticeService noticeService;

	@GetMapping("/")
	public String main(Model model, Locale locale) {
		// 반즈케 표 자체는 JS가 /api/banzuke로 비동기 로드한다. 여기서는 바쇼 선택 드롭다운 옵션만 내려준다
		// (맨 앞이 최신 바쇼 = 기본 선택).
		model.addAttribute("bashoOptions", banzukeService.listBashoOptions());
		// 바쇼 기간(개막 이틀 전 ~ 센슈라쿠 다음 날)에는 결과를 보러 오는 사람이 많아서 대진 탭을 먼저 연다. 그 밖에는 반즈케.
		model.addAttribute("defaultTab", banzukeService.isBashoPeriod(LocalDate.now(BASHO_ZONE)) ? "torikumi" : "banzuke");
		// "일문"/"키마리테" 탭은 반즈케 탭과 달리 바쇼·계급 선택이 없어서 서버에서 통째로 렌더링해 내려준다.
		model.addAttribute("ichimonGroups", banzukeService.getIchimonStructure());
		model.addAttribute("kimariteGroups", kimariteEncyclopediaService.getKimariteEncyclopedia());
		// 팝업 공지 (없으면 null). "오늘 하루 보지 않기" 여부는 브라우저가 기억하므로 띄울지는 JS가 정한다.
		model.addAttribute("notice", noticeService.findActive(locale).orElse(null));
		return "index";
	}
}
