package com.torikumilab.sumoarchive.domain.entity;

import com.torikumilab.sumoarchive.domain.entity.constant.FeedbackCategory;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 방문자 의견·오류 보고 (V8). 공개되지 않고 관리자 「의견함」에서만 본다. 보낸 사람의 IP는 저장하지 않는다.
 */
@Entity
@Table(name = "feedback")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeedbackEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Enumerated(EnumType.STRING)
	@Column(name = "category", nullable = false)
	private FeedbackCategory category;

	@Column(name = "content", nullable = false, length = 1000)
	private String content;

	// 답장을 원하는 사람만 적는 이메일 등 (선택)
	@Column(name = "contact", length = 100)
	private String contact;

	// 의견을 보낸 페이지 (푸터 링크가 ?from=으로 넘겨줌). 사이트 내부 경로만.
	@Column(name = "page_path", length = 255)
	private String pagePath;

	// 보낸 화면 언어 (ko/ja) - 답장 언어 참고용
	@Column(name = "lang", length = 5)
	private String lang;

	@Column(name = "handled", nullable = false)
	private boolean handled;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@Builder
	private FeedbackEntity(FeedbackCategory category, String content, String contact, String pagePath, String lang) {
		this.category = category;
		this.content = content;
		this.contact = contact;
		this.pagePath = pagePath;
		this.lang = lang;
		this.handled = false;
	}

	public void setHandled(boolean handled) {
		this.handled = handled;
	}
}
