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

	/** 작성자 본인 삭제 (비밀번호 검증은 서비스에서 끝낸 뒤 호출) */
	public void softDeleteByUser() {
		this.isDeleted = true;
		this.deletedBy = DeletedBy.USER;
	}

	/** 관리자 블라인드 처리 (비밀번호 검증 없이) */
	public void blindByAdmin() {
		this.isDeleted = true;
		this.deletedBy = DeletedBy.ADMIN;
	}
}
