package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.domain.dto.AdminCommentDTO;
import com.torikumilab.sumoarchive.domain.dto.CommentDTO;
import com.torikumilab.sumoarchive.domain.entity.BashoEntity;
import com.torikumilab.sumoarchive.domain.entity.CommentEntity;
import com.torikumilab.sumoarchive.domain.entity.TorikumiEntity;
import com.torikumilab.sumoarchive.domain.entity.constant.DeletedBy;
import com.torikumilab.sumoarchive.repository.CommentRepository;
import com.torikumilab.sumoarchive.repository.TorikumiRepository;
import com.torikumilab.sumoarchive.service.exception.PasswordMismatchException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 토리쿠미 익명 댓글 CRUD. 기능명세서 5번의 유효성 규칙(닉네임 8자 / 비밀번호 숫자 4자리 /
 * 내용 200자·공백 불가)과 소프트 딜리트 정책을 여기서 강제한다.
 *
 * <p>비밀번호는 4자리 PIN이라 사실상 전수 대입이 가능하지만, 명세서가 "경량 구현"을 전제로 하므로
 * 최소한 평문 저장은 피하려고 SHA-256 해시로만 저장/대조한다.</p>
 */
@Service
@RequiredArgsConstructor
public class CommentService {

	private static final int NICKNAME_MAX = 8;
	private static final int CONTENT_MAX = 200;
	private static final Pattern PIN_4 = Pattern.compile("\\d{4}");
	private static final DateTimeFormatter CREATED_AT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	private static final String MSG_USER_DELETED = "작성자에 의해 삭제된 댓글입니다";
	private static final String MSG_ADMIN_BLINDED = "관리자에 의해 블라인드 처리된 댓글입니다";

	private final CommentRepository commentRepository;
	private final TorikumiRepository torikumiRepository;

	@Transactional(readOnly = true)
	public List<CommentDTO> getComments(Integer torikumiId) {
		return commentRepository.findByTorikumiId(torikumiId).stream()
				.map(CommentService::toDto)
				.toList();
	}

	/** 댓글 등록 후, 갱신된 전체 목록을 반환(프론트가 그대로 다시 렌더링). */
	@Transactional
	public List<CommentDTO> addComment(Integer torikumiId, String nickname, String password, String content) {
		String nick = nickname == null ? "" : nickname.strip();
		String pin = password == null ? "" : password.strip();
		String body = content == null ? "" : content.strip();

		if (nick.isEmpty()) {
			throw new IllegalArgumentException("닉네임을 입력해 주세요.");
		}
		if (nick.length() > NICKNAME_MAX) {
			throw new IllegalArgumentException("닉네임은 최대 " + NICKNAME_MAX + "자입니다.");
		}
		if (!PIN_4.matcher(pin).matches()) {
			throw new IllegalArgumentException("비밀번호는 숫자 4자리로 입력해 주세요.");
		}
		if (body.isEmpty()) {
			throw new IllegalArgumentException("댓글 내용을 입력해 주세요.");
		}
		if (body.length() > CONTENT_MAX) {
			throw new IllegalArgumentException("댓글은 최대 " + CONTENT_MAX + "자입니다.");
		}

		TorikumiEntity torikumi = torikumiRepository.findById(torikumiId)
				.orElseThrow(() -> new EntityNotFoundException("토리쿠미를 찾을 수 없습니다. id=" + torikumiId));

		commentRepository.save(CommentEntity.builder()
				.torikumiEntity(torikumi)
				.nickName(nick)
				.password(sha256(pin))
				.content(body)
				.build());

		return getComments(torikumiId);
	}

	/** 작성자 본인 삭제: 비밀번호 일치 시 소프트 딜리트. 갱신된 목록 반환. */
	@Transactional
	public List<CommentDTO> deleteByUser(Integer torikumiId, Integer commentId, String password) {
		CommentEntity comment = loadCommentOfTorikumi(torikumiId, commentId);

		if (comment.isDeleted()) {
			// 이미 지워진 댓글이면 조용히 현재 상태 반환 (중복 클릭 방어)
			return getComments(torikumiId);
		}
		String pin = password == null ? "" : password.strip();
		if (!PIN_4.matcher(pin).matches() || !comment.getPassword().equals(sha256(pin))) {
			throw new PasswordMismatchException("비밀번호가 일치하지 않습니다.");
		}
		comment.softDeleteByUser();
		return getComments(torikumiId);
	}

	/** 관리자 블라인드: 비밀번호 검증 없이 가림 처리. 호출 전 관리자 세션 확인은 컨트롤러 책임. */
	@Transactional
	public List<CommentDTO> blindByAdmin(Integer torikumiId, Integer commentId) {
		CommentEntity comment = loadCommentOfTorikumi(torikumiId, commentId);
		if (!comment.isDeleted()) {
			comment.blindByAdmin();
		}
		return getComments(torikumiId);
	}

	/** 관리자 댓글 관리 대시보드(/admin/comments) - 사이트 전체 댓글을 최신순으로. */
	@Transactional(readOnly = true)
	public Page<AdminCommentDTO> getAllCommentsForAdmin(Pageable pageable) {
		return commentRepository.findAllForAdmin(pageable).map(CommentService::toAdminDto);
	}

	private CommentEntity loadCommentOfTorikumi(Integer torikumiId, Integer commentId) {
		CommentEntity comment = commentRepository.findById(commentId)
				.orElseThrow(() -> new EntityNotFoundException("댓글을 찾을 수 없습니다. id=" + commentId));
		if (!comment.getTorikumiEntity().getId().equals(torikumiId)) {
			throw new EntityNotFoundException("해당 토리쿠미의 댓글이 아닙니다. commentId=" + commentId);
		}
		return comment;
	}

	private static CommentDTO toDto(CommentEntity c) {
		boolean deleted = c.isDeleted();
		boolean blinded = c.getDeletedBy() == DeletedBy.ADMIN;
		String display;
		if (!deleted) {
			display = c.getContent();
		} else {
			display = blinded ? MSG_ADMIN_BLINDED : MSG_USER_DELETED;
		}
		LocalDateTime createdAt = c.getCreatedAt() != null ? c.getCreatedAt() : LocalDateTime.now();
		return new CommentDTO(
				c.getId(),
				deleted ? null : c.getNickName(),
				display,
				createdAt.format(CREATED_AT_FMT),
				deleted,
				blinded
		);
	}

	private static AdminCommentDTO toAdminDto(CommentEntity c) {
		CommentDTO base = toDto(c);
		TorikumiEntity t = c.getTorikumiEntity();
		return new AdminCommentDTO(
				base.id(), t.getId(), matchLabelOf(t),
				base.nickName(), base.displayContent(), base.createdAt(),
				base.deleted(), base.blinded()
		);
	}

	private static String matchLabelOf(TorikumiEntity t) {
		BashoEntity basho = t.getBashoEntity();
		String bashoTitle = basho.getBashoYear() + "년 " + basho.getBashoMonth().getMonthValue()
				+ "월 " + basho.getBashoMonth().getDisplayNameKr();
		String east = firstToken(t.getEastRikishiEntity().getShikonaKr());
		String west = firstToken(t.getWestRikishiEntity().getShikonaKr());
		return bashoTitle + " " + t.getDay() + "일째 · " + east + " vs " + west;
	}

	private static String firstToken(String value) {
		if (value == null) {
			return null;
		}
		int idx = value.indexOf(' ');
		return idx > 0 ? value.substring(0, idx) : value;
	}

	private static String sha256(String raw) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(digest.length * 2);
			for (byte b : digest) {
				sb.append(Character.forDigit((b >> 4) & 0xF, 16));
				sb.append(Character.forDigit(b & 0xF, 16));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 미지원 환경", e);
		}
	}
}
