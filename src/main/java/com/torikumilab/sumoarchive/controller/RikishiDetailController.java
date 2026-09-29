package com.torikumilab.sumoarchive.controller;

import com.torikumilab.sumoarchive.domain.dto.BashoGameLogDTO;
import com.torikumilab.sumoarchive.domain.dto.HeadToHeadDTO;
import com.torikumilab.sumoarchive.domain.dto.HeadToHeadGroupDTO;
import com.torikumilab.sumoarchive.domain.dto.KimariteStatDTO;
import com.torikumilab.sumoarchive.domain.dto.RikishiDetailDTO;
import com.torikumilab.sumoarchive.service.RikishiAnalysisService;
import com.torikumilab.sumoarchive.service.RikishiDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * SearchViewController와 동일하게 "화면(뷰) 컨트롤러"라 controller.api가 아닌 controller 패키지에 뒀습니다.
 */
@Controller
@RequiredArgsConstructor
public class RikishiDetailController {

	private final RikishiDetailService rikishiDetailService;
	private final RikishiAnalysisService rikishiAnalysisService;

	@GetMapping("/rikishi/{id}")
	public String detail(@PathVariable Integer id,
						  @RequestParam(required = false) Integer fromBasho,
						  @RequestParam(required = false) Integer toBasho,
						  Model model) {
		RikishiDetailDTO rikishi = rikishiDetailService.getRikishiDetail(id);
		List<KimariteStatDTO> kimariteStats = rikishiDetailService.getKimariteStats(id, fromBasho, toBasho);
		List<KimariteStatDTO> kimariteLossStats = rikishiDetailService.getKimariteLossStats(id, fromBasho, toBasho);
		List<BashoGameLogDTO> gameLog = rikishiDetailService.getGameLog(id);
		// 상대전적은 한 번만 조회해서 초성 그룹과 "유독 약한/강한 상대" 요약에 같이 쓴다.
		List<HeadToHeadDTO> headToHead = rikishiDetailService.getHeadToHead(id);
		List<HeadToHeadGroupDTO> headToHeadGroups = rikishiDetailService.groupHeadToHead(headToHead);

		model.addAttribute("rikishi", rikishi);
		model.addAttribute("kimariteStats", kimariteStats);
		model.addAttribute("kimariteLossStats", kimariteLossStats);
		// 패배 유형은 키마리테 차트(패배 모드) 아래에 붙으므로 같은 기간 필터를 따른다. 상대 조건별 성적은 통산.
		model.addAttribute("lossTypes", rikishiAnalysisService.getLossTypes(id, fromBasho, toBasho));
		model.addAttribute("opponentConditions", rikishiAnalysisService.getOpponentConditions(id));
		model.addAttribute("gameLog", gameLog);
		// 프로필 상단 요약 줄의 "최근 바쇼" - 대전 기록이 적재된 가장 최근 바쇼 (적재 전 바쇼는 recordSummary가 "-")
		model.addAttribute("recentBasho", gameLog.stream()
				.filter(g -> !"-".equals(g.recordSummary()))
				.findFirst()
				.orElse(null));
		model.addAttribute("headToHeadGroups", headToHeadGroups);
		model.addAttribute("headToHeadHighlights", rikishiAnalysisService.getHeadToHeadHighlights(id, headToHead));
		// 결정기술 차트 기간 필터 select 기본 선택값 유지용 (없으면 "전체" 옵션이 선택됨)
		model.addAttribute("fromBasho", fromBasho);
		model.addAttribute("toBasho", toBasho);
		return "rikishi/detail";
	}
}
