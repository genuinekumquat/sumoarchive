package com.torikumilab.sumoarchive.controller.api;

import com.torikumilab.sumoarchive.domain.dto.CommentDTO;
import com.torikumilab.sumoarchive.service.CommentService;
import com.torikumilab.sumoarchive.service.exception.AdminOnlyException;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 토리쿠미 익명 댓글 API. 슬라이드 패널 / 전체 페이지 양쪽이 fetch로 호출하고,
 * 응답은 항상 "그 토리쿠미의 최신 댓글 목록 전체"라서 프론트는 받은 배열로 목록을 다시 그리기만 하면 된다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/torikumi/{torikumiId}/comments")
public class TorikumiCommentApiController {

	private final CommentService commentService;

	@GetMapping
	public List<CommentDTO> list(@PathVariable Integer torikumiId) {
		return commentService.getComments(torikumiId);
	}

	@PostMapping
	public ResponseEntity<List<CommentDTO>> create(
			@PathVariable Integer torikumiId,
			@RequestParam String nickname,
			@RequestParam String password,
			@RequestParam String content
	) {
		List<CommentDTO> comments = commentService.addComment(torikumiId, nickname, password, content);
		return ResponseEntity.status(HttpStatus.CREATED).body(comments);
	}

	/** 작성자 본인 삭제 (비밀번호 4자리 검증). */
	@PostMapping("/{commentId}/delete")
	public List<CommentDTO> delete(
			@PathVariable Integer torikumiId,
			@PathVariable Integer commentId,
			@RequestParam String password
	) {
		return commentService.deleteByUser(torikumiId, commentId, password);
	}

	/**
	 * 관리자 블라인드 처리. 세션에 isAdmin=true가 있어야 한다 (/admin/login에서 부여, AdminViewController).
	 */
	@PostMapping("/{commentId}/blind")
	public List<CommentDTO> blind(
			@PathVariable Integer torikumiId,
			@PathVariable Integer commentId,
			HttpSession session
	) {
		if (!Boolean.TRUE.equals(session.getAttribute("isAdmin"))) {
			throw new AdminOnlyException("관리자만 사용할 수 있습니다.");
		}
		return commentService.blindByAdmin(torikumiId, commentId);
	}
}
