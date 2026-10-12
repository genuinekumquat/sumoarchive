package com.torikumilab.sumoarchive.controller.api;

import com.torikumilab.sumoarchive.domain.dto.TorikumiDayDTO;
import com.torikumilab.sumoarchive.domain.entity.constant.Division;
import com.torikumilab.sumoarchive.service.TorikumiDayService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 메인 "대진" 탭이 날짜·디비전을 바꿀 때마다 부르는 API. bashoId·day를 빼면 최신 바쇼의 기본 일차. */
@RestController
@RequiredArgsConstructor
public class TorikumiDayApiController {

	private final TorikumiDayService torikumiDayService;

	@GetMapping("/api/torikumi/day")
	public TorikumiDayDTO getDay(@RequestParam(required = false) Integer bashoId,
								 @RequestParam(defaultValue = "Makuuchi") Division division,
								 @RequestParam(required = false) Integer day) {
		return torikumiDayService.getDay(bashoId, division, day);
	}
}
