package com.torikumilab.sumoarchive.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "heya")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 1. JPA 필수 기본 생성자 (보안 유지)
public class HeyaEntity {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	
	@Column(name = "name_kr", nullable = false, length = 100)
	private String nameKr;

	// 한국어 헤야명이 로마자 음차 자동값(검수 전)인지. null/true = 자동, false = 관리자가 저장해 검수됨.
	// RikishiEntity.shikonaKrAuto와 같은 하이브리드 규칙(자동 채우기 + 관리자 검수).
	@Column(name = "name_kr_auto")
	private Boolean nameKrAuto;

	@Column(name = "name_jp", nullable = false, length = 100)
	private String nameJp;

	// sumo-api의 로마자 헤야명 (예: "Takasago"). 임포트 매칭 키. 한/일명은 후속 슬라이스에서 보강.
	@Column(name = "name_en", length = 100)
	private String nameEn;
	
	@Column(name = "ichimon_kr", length = 100)
	private String ichimonKr;
	
	@Column(name = "ichimon_jp", length = 100)
	private String ichimonJp;
	
	// raw FK는 읽기 전용으로 선언
	@Column(name = "master_rikishi_id", insertable = false, updatable = false)
	private Integer masterRikishiId;
	
	// 연관관계는 그대로 유지
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "master_rikishi_id")
	private RikishiEntity masterRikishiEntity;
	
	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;  // 초 단위까지 저장되도록 LocalDateTime 변경
	
	// 최초 DB 데이터 적재 등을 위한 깔끔한 빌더 생성자 제공
	@Builder
	private HeyaEntity(String nameKr, String nameJp, String nameEn, String ichimonKr, String ichimonJp, RikishiEntity masterRikishiEntity) {
		this.nameKr = nameKr;
		this.nameJp = nameJp;
		this.nameEn = nameEn;
		this.ichimonKr = ichimonKr;
		this.ichimonJp = ichimonJp;
		this.masterRikishiEntity = masterRikishiEntity;
	}
	
	public void updateMasterRikishi(RikishiEntity masterRikishiEntity) {
		this.masterRikishiEntity = masterRikishiEntity;
	}

	/** {@code isNameKrAuto()} — null(임포트 직후, 미검수)도 자동으로 본다. */
	public boolean isNameKrAuto() {
		return !Boolean.FALSE.equals(nameKrAuto);
	}

	/** 자동 채우기: 로마자 음차로 만든 한국어 헤야명을 넣고 "자동" 표시를 켠다. */
	public void applyAutoNameKr(String nameKr) {
		this.nameKr = nameKr;
		this.nameKrAuto = true;
	}

	/** 관리자 검수: 한/일명을 저장하고 "자동" 표시를 끈다. */
	public void updateNames(String nameKr, String nameJp) {
		this.nameKr = nameKr;
		this.nameJp = nameJp;
		this.nameKrAuto = false;
	}
}
