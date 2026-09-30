package com.torikumilab.sumoarchive.domain.entity;

import com.torikumilab.sumoarchive.domain.entity.constant.DeletedBy;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "comment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "torikumi_id", nullable = false)
	private TorikumiEntity torikumiEntity;

	@Column(name = "nickname", nullable = false, length = 8)
	private String nickName;

	// 익명 댓글 삭제용 4자리 PIN. 평문이 아니라 SHA-256 해시로 저장한다(CommentService 참고).
	@Column(name = "password", nullable = false, length = 255)
	private String password;

	@Column(name = "content", nullable = false, length = 200)
	private String content;

	@Column(name = "is_deleted")
	private boolean isDeleted = false;

	// 소프트 딜리트된 경우에만 채워짐. 화면에 어떤 안내 문구를 보여줄지 결정한다.
	@Enumerated(EnumType.STRING)
	@Column(name = "deleted_by", length = 10)
	private DeletedBy deletedBy;

	// 방문자 신고 수. 자동으로 가리지는 않고, 관리자 화면에서 신고된 댓글을 모아 보고 블라인드 여부를 판단한다.
	// 동시 신고에도 누락이 없도록 값은 CommentRepository.incrementReportCount(UPDATE ... + 1)로만 올린다.
	@Column(name = "report_count", nullable = false)
	private int reportCount = 0;

	@Column(name = "last_reported_at")
	private LocalDateTime lastReportedAt;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@Builder
	private CommentEntity(TorikumiEntity torikumiEntity, String nickName, String password, String content) {
		this.torikumiEntity = torikumiEntity;
		this.nickName = nickName;
		this.password = password;
		this.content = content;
	}

	/**
	 * 작성자 본인 삭제 (비밀번호 검증은 서비스에서 끝낸 뒤 호출).
	 * 행은 "삭제된 댓글입니다" 자리 유지용으로 남기되, 닉네임·내용·비밀번호 해시는 지운다
	 * (개인정보 처리 안내: 본인이 지운 댓글은 원문을 보관하지 않음). 컬럼이 NOT NULL이라 빈 문자열.
	 */
	public void softDeleteByUser() {
		this.isDeleted = true;
		this.deletedBy = DeletedBy.USER;
		this.nickName = "";
		this.content = "";
		this.password = "";
	}

	/** 관리자 블라인드 처리 (비밀번호 검증 없이) */
	public void blindByAdmin() {
		this.isDeleted = true;
		this.deletedBy = DeletedBy.ADMIN;
	}
}
