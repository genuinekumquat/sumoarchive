package com.torikumilab.sumoarchive.controller.api;

import com.torikumilab.sumoarchive.domain.dto.search.RikishiSearchResultDTO;
import com.torikumilab.sumoarchive.service.RikishiSearchService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SearchApiController {
	
	private final RikishiSearchService searchService;
	
	public SearchApiController(RikishiSearchService searchService) {
		this.searchService = searchService;
	}
	
	@GetMapping("/api/search")
	public Page<RikishiSearchResultDTO> search(@RequestParam String keyword,
											   @RequestParam(defaultValue = "0") int page) {
		return searchService.search(keyword, page);
	}
}