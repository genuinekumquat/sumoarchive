package com.torikumilab.sumoarchive.controller;

import com.torikumilab.sumoarchive.domain.dto.BashoGameLogDTO;
import com.torikumilab.sumoarchive.domain.dto.KimariteStatDTO;
import com.torikumilab.sumoarchive.domain.dto.RikishiDetailDTO;
import com.torikumilab.sumoarchive.service.RikishiDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * SearchViewController와 동일하게 "화면(뷰) 컨트롤러"라 controller.api가 아닌 controller 패키지에 뒀습니다.
 */
@Controller
@RequiredArgsConstructor
public class RikishiDetailController {
	
	private final RikishiDetailService rikishiDetailService;
	
	@GetMapping("/rikishi/{id}")
	public String detail(@PathVariable Integer id, Model model) {
		RikishiDetailDTO rikishi = rikishiDetailService.getRikishiDetail(id);
		List<KimariteStatDTO> kimariteStats = rikishiDetailService.getKimariteStats(id);
		List<BashoGameLogDTO> gameLog = rikishiDetailService.getGameLog(id);
		
		model.addAttribute("rikishi", rikishi);
		model.addAttribute("kimariteStats", kimariteStats);
		model.addAttribute("gameLog", gameLog);
		return "rikishi/detail";
	}
}