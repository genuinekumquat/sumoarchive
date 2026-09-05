package com.torikumilab.sumoarchive.controller;

import com.torikumilab.sumoarchive.domain.dto.AdminCommentDTO;
import com.torikumilab.sumoarchive.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 관리자 댓글 관리 대시보드. /admin/** 경로라 AdminAuthInterceptor(WebConfig)가
 * 세션 isAdmin 여부를 먼저 검사한다.
 */
@Controller
@RequiredArgsConstructor
public class AdminCommentViewController {

	private static final int PAGE_SIZE = 30;

	private final CommentService commentService;

	@GetMapping("/admin/comments")
	public String comments(@RequestParam(defaultValue = "0") int page, Model model) {
		Page<AdminCommentDTO> result = commentService.getAllCommentsForAdmin(
				PageRequest.of(page, PAGE_SIZE, Sort.unsorted()));
		model.addAttribute("result", result);
		return "admin/comments";
	}
}
