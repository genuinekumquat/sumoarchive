package com.torikumilab.sumoarchive.controller;

import com.torikumilab.sumoarchive.domain.dto.search.RikishiSearchResultDTO;
import com.torikumilab.sumoarchive.service.RikishiSearchService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class SearchViewController {
	
	private final RikishiSearchService searchService;
	
	public SearchViewController(RikishiSearchService searchService) {
		this.searchService = searchService;
	}
	
	@GetMapping("/search")
	public String search(@RequestParam(required = false) String keyword,
						 @RequestParam(defaultValue = "0") int page,
						 Model model) {
		Page<RikishiSearchResultDTO> result = searchService.search(keyword, page);
		model.addAttribute("keyword", keyword);
		model.addAttribute("result", result);
		return "search/result";
	}
}