package com.torikumilab.sumoarchive.controller.api;

import com.torikumilab.sumoarchive.domain.dto.RikishiBookmarkCardDTO;
import com.torikumilab.sumoarchive.service.RikishiBookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 북마크 페이지가 LocalStorage의 리키시 ID 배열을 던져서 카드 정보를 받아가는 API.
 * 기능명세서에 명시된 경로(GET /api/rikishiEntity?ids=3,15,24)를 그대로 따른다.
 */
@RestController
@RequiredArgsConstructor
public class RikishiBookmarkApiController {

	private final RikishiBookmarkService rikishiBookmarkService;

	@GetMapping("/api/rikishiEntity")
	public List<RikishiBookmarkCardDTO> getByIds(@RequestParam List<Integer> ids) {
		return rikishiBookmarkService.getBookmarkCards(ids);
	}
}
