package com.torikumilab.sumoarchive.controller;

import com.torikumilab.sumoarchive.service.CommentService;
import com.torikumilab.sumoarchive.service.TorikumiDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 토리쿠미(경기) 상세 화면 컨트롤러. RikishiDetailController와 마찬가지로 "뷰 컨트롤러"라
 * controller.api가 아닌 controller 패키지에 둔다.
 *
 * <ul>
 *   <li>{@code GET /torikumi/{id}}          – 독립 전체 페이지</li>
 *   <li>{@code GET /torikumi/{id}/fragment} – 리키시 프로필 호시토리표에서 여는 슬라이드 패널용 조각.
 *       전체 페이지와 같은 템플릿의 {@code content} 프래그먼트를 그대로 재사용한다(중복 제거).</li>
 * </ul>
 */
@Controller
@RequiredArgsConstructor
public class TorikumiViewController {

	private final TorikumiDetailService torikumiDetailService;
	private final CommentService commentService;

	@GetMapping("/torikumi/{id}")
	public String detail(@PathVariable Integer id, Model model) {
		populate(id, model);
		return "torikumi/detail";
	}

	@GetMapping("/torikumi/{id}/fragment")
	public String fragment(@PathVariable Integer id, Model model) {
		populate(id, model);
		return "torikumi/detail :: content";
	}

	private void populate(Integer id, Model model) {
		model.addAttribute("torikumi", torikumiDetailService.getTorikumiDetail(id));
		model.addAttribute("comments", commentService.getComments(id));
	}
}
