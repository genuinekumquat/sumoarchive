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

	/** reported=true면 처리 안 된 신고 댓글만 신고 많은 순으로 (기본은 전체 최신순). */
	@GetMapping("/admin/comments")
	public String comments(@RequestParam(defaultValue = "0") int page,
						   @RequestParam(defaultValue = "false") boolean reported,
						   Model model) {
		PageRequest pageable = PageRequest.of(page, PAGE_SIZE, Sort.unsorted());
		Page<AdminCommentDTO> result = reported
				? commentService.getReportedCommentsForAdmin(pageable)
				: commentService.getAllCommentsForAdmin(pageable);
		model.addAttribute("result", result);
		model.addAttribute("reported", reported);
		model.addAttribute("pendingReportedCount", commentService.countPendingReported());
		return "admin/comments";
	}
}
