package com.torikumilab.sumoarchive.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 메인 화면 팝업 공지 (V7). 관리자가 /admin/notice에서 관리하고, 켜진 공지 중 가장 최근 것 하나만 뜬다.
 * 방문자의 "다시 보지 않기"는 브라우저에 공지 id로 기억하므로, 다시 띄우고 싶으면 새 공지를 만든다.
 */
@Entity
@Table(name = "notice")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoticeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(name = "title_ko", nullable = false, length = 100)
	private String titleKo;

	@Column(name = "body_ko", nullable = false, length = 1000)
	private String bodyKo;

	@Column(name = "title_ja", length = 100)
	private String titleJa;

	@Column(name = "body_ja", length = 1000)
	private String bodyJa;

	// 팝업의 "자세히 보기" 버튼 주소. 사이트 내부 경로(/updates) 또는 https:// 주소만 받는다.
	@Column(name = "link_url", length = 255)
	private String linkUrl;

	@Column(name = "active", nullable = false)
	private boolean active;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@Builder
	private NoticeEntity(String titleKo, String bodyKo, String titleJa, String bodyJa, String linkUrl, boolean active) {
		this.titleKo = titleKo;
		this.bodyKo = bodyKo;
		this.titleJa = titleJa;
		this.bodyJa = bodyJa;
		this.linkUrl = linkUrl;
		this.active = active;
	}

	public void update(String titleKo, String bodyKo, String titleJa, String bodyJa, String linkUrl) {
		this.titleKo = titleKo;
		this.bodyKo = bodyKo;
		this.titleJa = titleJa;
		this.bodyJa = bodyJa;
		this.linkUrl = linkUrl;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
}
