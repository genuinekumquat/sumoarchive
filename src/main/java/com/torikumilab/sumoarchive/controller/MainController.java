package com.torikumilab.sumoarchive.controller;

import com.torikumilab.sumoarchive.domain.entity.constant.Division;
import com.torikumilab.sumoarchive.service.BanzukeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class MainController {
	
	private final BanzukeService banzukeService;
	
	@GetMapping("/")
	public String main(Model model) {
		model.addAttribute("banzukeList", banzukeService.getLatestBanzuke(Division.Makuuchi));
		return "index";
	}
}
