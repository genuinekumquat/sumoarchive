package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.domain.dto.CommentDTO;
import com.torikumilab.sumoarchive.domain.entity.CommentEntity;
import com.torikumilab.sumoarchive.domain.entity.TorikumiEntity;
import com.torikumilab.sumoarchive.domain.entity.constant.DeletedBy;
import com.torikumilab.sumoarchive.repository.CommentRepository;
import com.torikumilab.sumoarchive.repository.TorikumiRepository;
import com.torikumilab.sumoarchive.service.exception.PasswordMismatchException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

	private static final int TORIKUMI_ID = 100;
	private static final int COMMENT_ID = 7;
	// "1234"의 SHA-256
	private static final String PIN_1234_HASH = "03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4";

	@Mock
	private CommentRepository commentRepository;
	@Mock
	private TorikumiRepository torikumiRepository;

	@InjectMocks
	private CommentService commentService;

	private static TorikumiEntity torikumi(int id) {
		TorikumiEntity t = TorikumiEntity.builder().build();
		ReflectionTestUtils.setField(t, "id", id);
		return t;
	}

	/** 저장돼 있는 댓글 (id·작성 시각은 DB가 채우는 값이라 리플렉션으로 넣는다) */
	private static CommentEntity savedComment(int torikumiId) {
		CommentEntity c = CommentEntity.builder()
				.torikumiEntity(torikumi(torikumiId))
				.nickName("스모팬")
				.password(PIN_1234_HASH)
				.content("좋은 경기였다")
				.build();
		ReflectionTestUtils.setField(c, "id", COMMENT_ID);
		ReflectionTestUtils.setField(c, "createdAt", LocalDateTime.of(2026, 9, 2, 14, 3));
		return c;
	}

	// ===== 작성 =====

	@Test
	@DisplayName("작성: 앞뒤 공백을 자르고, 비밀번호는 평문이 아닌 SHA-256 해시로 저장")
	void addComment_trimsInputAndHashesPin() {
		given(torikumiRepository.findById(TORIKUMI_ID)).willReturn(Optional.of(torikumi(TORIKUMI_ID)));

		commentService.addComment(TORIKUMI_ID, "  스모팬 ", " 1234 ", "  좋은 경기였다  ");

		ArgumentCaptor<CommentEntity> saved = ArgumentCaptor.forClass(CommentEntity.class);
		verify(commentRepository).save(saved.capture());
		assertThat(saved.getValue().getNickName()).isEqualTo("스모팬");
		assertThat(saved.getValue().getContent()).isEqualTo("좋은 경기였다");
		assertThat(saved.getValue().getPassword()).isEqualTo(PIN_1234_HASH).isNotEqualTo("1234");
	}

	@Test
	@DisplayName("작성: 닉네임 빈값·8자 초과, 비밀번호 숫자 4자리 아님, 내용 빈값·200자 초과는 거부하고 저장하지 않음")
	void addComment_rejectsInvalidInput() {
		String ok = "내용";
		assertInvalid(" ", "1234", ok, "comment.error.nickname.required");
		assertInvalid(null, "1234", ok, "comment.error.nickname.required");
		assertInvalid("가나다라마바사아자", "1234", ok, "comment.error.nickname.length");
		assertInvalid("팬", "123", ok, "comment.error.pin.format");
		assertInvalid("팬", "12345", ok, "comment.error.pin.format");
		assertInvalid("팬", "abcd", ok, "comment.error.pin.format");
		assertInvalid("팬", "1234", "   ", "comment.error.content.required");
		assertInvalid("팬", "1234", "가".repeat(201), "comment.error.content.length");

		verify(commentRepository, never()).save(any());
	}

	private void assertInvalid(String nickname, String pin, String content, String expectedCode) {
		assertThatThrownBy(() -> commentService.addComment(TORIKUMI_ID, nickname, pin, content))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage(expectedCode);
	}

	@Test
	@DisplayName("작성: 경계값(닉네임 8자, 내용 200자)은 허용")
	void addComment_acceptsBoundaryLengths() {
		given(torikumiRepository.findById(TORIKUMI_ID)).willReturn(Optional.of(torikumi(TORIKUMI_ID)));

		commentService.addComment(TORIKUMI_ID, "가나다라마바사아", "0000", "가".repeat(200));

		verify(commentRepository).save(any(CommentEntity.class));
	}

	@Test
	@DisplayName("작성: 없는 토리쿠미면 404(EntityNotFoundException)")
	void addComment_whenTorikumiMissing_throwsNotFound() {
		given(torikumiRepository.findById(TORIKUMI_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> commentService.addComment(TORIKUMI_ID, "팬", "1234", "내용"))
				.isInstanceOf(EntityNotFoundException.class);
	}

	// ===== 본인 삭제 =====

	@Test
	@DisplayName("본인 삭제: 비밀번호가 맞으면 작성자 삭제 상태가 된다")
	void deleteByUser_withCorrectPin_softDeletes() {
		CommentEntity c = savedComment(TORIKUMI_ID);
		given(commentRepository.findById(COMMENT_ID)).willReturn(Optional.of(c));

		commentService.deleteByUser(TORIKUMI_ID, COMMENT_ID, "1234");

		assertThat(c.isDeleted()).isTrue();
		assertThat(c.getDeletedBy()).isEqualTo(DeletedBy.USER);
	}

	@Test
	@DisplayName("본인 삭제: 비밀번호가 틀리거나 형식이 아니면 PasswordMismatchException, 댓글은 그대로")
	void deleteByUser_withWrongPin_throwsAndKeepsComment() {
		CommentEntity c = savedComment(TORIKUMI_ID);
		given(commentRepository.findById(COMMENT_ID)).willReturn(Optional.of(c));

		assertThatThrownBy(() -> commentService.deleteByUser(TORIKUMI_ID, COMMENT_ID, "9999"))
				.isInstanceOf(PasswordMismatchException.class);
		assertThatThrownBy(() -> commentService.deleteByUser(TORIKUMI_ID, COMMENT_ID, null))
				.isInstanceOf(PasswordMismatchException.class);

		assertThat(c.isDeleted()).isFalse();
	}

	@Test
	@DisplayName("본인 삭제: 이미 지워진 댓글은 비밀번호와 상관없이 조용히 현재 목록 반환 (중복 클릭 방어)")
	void deleteByUser_whenAlreadyDeleted_returnsQuietly() {
		CommentEntity c = savedComment(TORIKUMI_ID);
		c.blindByAdmin();
		given(commentRepository.findById(COMMENT_ID)).willReturn(Optional.of(c));
		given(commentRepository.findByTorikumiId(TORIKUMI_ID)).willReturn(List.of(c));

		List<CommentDTO> result = commentService.deleteByUser(TORIKUMI_ID, COMMENT_ID, "9999");

		assertThat(result).hasSize(1);
		assertThat(c.getDeletedBy()).isEqualTo(DeletedBy.ADMIN); // 관리자 블라인드가 본인 삭제로 바뀌지 않음
	}

	@Test
	@DisplayName("본인 삭제·블라인드·신고: 다른 토리쿠미의 댓글 id로 요청하면 404")
	void actions_whenCommentBelongsToOtherTorikumi_throwNotFound() {
		given(commentRepository.findById(COMMENT_ID)).willReturn(Optional.of(savedComment(999)));

		assertThatThrownBy(() -> commentService.deleteByUser(TORIKUMI_ID, COMMENT_ID, "1234"))
				.isInstanceOf(EntityNotFoundException.class);
		assertThatThrownBy(() -> commentService.blindByAdmin(TORIKUMI_ID, COMMENT_ID))
				.isInstanceOf(EntityNotFoundException.class);
		assertThatThrownBy(() -> commentService.report(TORIKUMI_ID, COMMENT_ID))
				.isInstanceOf(EntityNotFoundException.class);
	}

	// ===== 관리자 블라인드 =====

	@Test
	@DisplayName("블라인드: 관리자 블라인드 상태가 되고, 이미 본인이 지운 댓글은 본인 삭제로 남는다")
	void blindByAdmin_blindsLiveCommentOnly() {
		CommentEntity live = savedComment(TORIKUMI_ID);
		given(commentRepository.findById(COMMENT_ID)).willReturn(Optional.of(live));
		commentService.blindByAdmin(TORIKUMI_ID, COMMENT_ID);
		assertThat(live.getDeletedBy()).isEqualTo(DeletedBy.ADMIN);

		CommentEntity userDeleted = savedComment(TORIKUMI_ID);
		userDeleted.softDeleteByUser();
		given(commentRepository.findById(COMMENT_ID)).willReturn(Optional.of(userDeleted));
		commentService.blindByAdmin(TORIKUMI_ID, COMMENT_ID);
		assertThat(userDeleted.getDeletedBy()).isEqualTo(DeletedBy.USER);
	}

	// ===== 신고 =====

	@Test
	@DisplayName("신고: 살아 있는 댓글만 신고 수를 올리고, 지워진 댓글 신고는 무시")
	void report_incrementsOnlyForLiveComment() {
		CommentEntity live = savedComment(TORIKUMI_ID);
		given(commentRepository.findById(COMMENT_ID)).willReturn(Optional.of(live));
		commentService.report(TORIKUMI_ID, COMMENT_ID);
		verify(commentRepository).incrementReportCount(eq(COMMENT_ID), any(LocalDateTime.class));

		CommentEntity deleted = savedComment(TORIKUMI_ID);
		deleted.softDeleteByUser();
		given(commentRepository.findById(COMMENT_ID)).willReturn(Optional.of(deleted));
		commentService.report(TORIKUMI_ID, COMMENT_ID);
		verify(commentRepository).incrementReportCount(anyInt(), any()); // 여전히 1번뿐
	}

	// ===== 화면용 변환 =====

	@Test
	@DisplayName("목록: 삭제된 댓글은 닉네임·원문을 내려주지 않고 안내 문구로 바꾼다 (본인 삭제·블라인드 문구 구분)")
	void getComments_hidesDeletedContent() {
		CommentEntity live = savedComment(TORIKUMI_ID);
		CommentEntity userDeleted = savedComment(TORIKUMI_ID);
		userDeleted.softDeleteByUser();
		CommentEntity blinded = savedComment(TORIKUMI_ID);
		blinded.blindByAdmin();
		given(commentRepository.findByTorikumiId(TORIKUMI_ID)).willReturn(List.of(live, userDeleted, blinded));

		List<CommentDTO> result = commentService.getComments(TORIKUMI_ID);

		assertThat(result.get(0).nickName()).isEqualTo("스모팬");
		assertThat(result.get(0).displayContent()).isEqualTo("좋은 경기였다");
		assertThat(result.get(0).createdAt()).isEqualTo("2026-09-02 14:03");

		assertThat(result.get(1).nickName()).isNull();
		assertThat(result.get(1).displayContent()).isEqualTo("작성자에 의해 삭제된 댓글입니다");
		assertThat(result.get(1).blinded()).isFalse();

		assertThat(result.get(2).nickName()).isNull();
		assertThat(result.get(2).displayContent()).isEqualTo("관리자에 의해 블라인드 처리된 댓글입니다");
		assertThat(result.get(2).blinded()).isTrue();

		assertThat(result).noneMatch(dto -> dto.deleted() && "좋은 경기였다".equals(dto.displayContent()));
	}
}
