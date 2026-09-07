package com.torikumilab.sumoarchive.controller.api;

import com.torikumilab.sumoarchive.domain.dto.BanzukeDTO;
import com.torikumilab.sumoarchive.domain.entity.constant.Division;
import com.torikumilab.sumoarchive.service.BanzukeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class BanzukeApiController {
	/*
	
	시점 1: 유저가 사이트에 처음 접속 (주소창에 도메인 입력, 엔터)
   → MainController가 담당
   → HTML 페이지 전체(index.html)를 그려서 응답
   → 이 안에 마쿠우치 데이터가 기본값으로 이미 박혀서 나감

	시점 2: 이미 화면을 보고 있는 상태에서, 유저가 "주료" 탭 클릭
   → BanzukeApiController가 담당
   → HTML이 아니라 JSON 데이터만 응답
   → 화면은 안 바뀌고, JS가 그 데이터로 표(테이블)만 갈아끼움
   
   
   BanzukeApiController : "탭 클릭"이라는 이벤트에 대응하는 부분만 담당
	
	 */
	
	private final BanzukeService banzukeService;
	
	@GetMapping("/api/banzuke")
	public List<BanzukeDTO> getBanzuke(
			@RequestParam Division division,
			@RequestParam(required = false) Integer bashoId
			) {
		return banzukeService.getBanzuke(bashoId, division);
	}
}
