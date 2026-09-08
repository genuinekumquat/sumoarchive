package com.torikumilab.sumoarchive.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "rikishi")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RikishiEntity {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	
	@Column(name = "external_api_id", unique = true)
	private Integer externalApiId;
	
	@Column(name = "shikona_kr", length = 100)
	private String shikonaKr;

	// 한국어 시코나가 음차 자동 생성값인지(관리자 검수 전) 여부. 관리자가 프로필에서 저장하면 false.
	// null = 자동 아님(수동 입력 또는 미입력). 슬라이스 5.
	@Column(name = "shikona_kr_auto")
	private Boolean shikonaKrAuto;

	@Column(name = "shikona_jp", length = 100)
	private String shikonaJp;

	// sumo-api의 로마자 시코나 (예: "Asanoyama"). 임포트 매칭·로마자 검색용. 한국어 시코나는 별도로 채운다.
	@Column(name = "shikona_en", length = 100)
	private String shikonaEn;
	
	@Column(name = "name", length = 100)
	private String name;
	
	@Column(name = "birthdate")
	private LocalDate birthdate; // Date ➔ LocalDate 변경
	
	@Column(name = "birthplace", length = 255)
	private String birthplace;
	
	@Column(name = "nationality", length = 100)
	private String nationality;
	
	@Column(name = "height", precision = 4, scale = 1)
	private BigDecimal height; // Integer ➔ BigDecimal 변경
	
	@Column(name = "weight", precision = 4, scale = 1)
	private BigDecimal weight; // Integer ➔ BigDecimal 변경
	
	@ManyToOne(fetch = FetchType.LAZY) // 연관관계 매핑 (성능 최적화를 위해 LAZY 권장)
	@JoinColumn(name = "heya_id")      // 실제 DB 테이블의 FK 컬럼명
	private HeyaEntity heyaEntity;                 // Integer heyaId ➔ Heya 객체로 변환
	
	@Column(name = "current_rank", length = 50)
	private String currentRank;
	
	@Column(name = "highest_rank", length = 50)
	private String highestRank;
	
	@Column(name = "fighting_style", length = 255)
	private String fightingStyle;
	
	@Column(name = "debut_date")
	private LocalDate debutDate; // Date ➔ LocalDate 변경
	
	@Column(name = "is_active", nullable = false)
	private boolean isActive = true; // 기본값 true 세팅
	
	@Column(name = "retired_date")
	private LocalDate retiredDate; // Date ➔ LocalDate 변경
	
	@Column(name = "oyakata_name_kr", length = 100)
	private String oyakataNameKr;
	
	@Column(name = "oyakata_name_jp", length = 100)
	private String oyakataNameJp;
	
	@Column(name = "photo_url", length = 255)
	private String photoUrl;
	
	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;
	
	@Builder
	private RikishiEntity(
			Integer externalApiId,
			String shikonaKr,
			String shikonaJp,
			String shikonaEn,
			String name,
			LocalDate birthdate,
			String birthplace,
			String nationality,
			BigDecimal height,
			BigDecimal weight,
			HeyaEntity heyaEntity,
			String currentRank,
			String highestRank,
			String fightingStyle,
			LocalDate debutDate,
			Boolean isActive,
			LocalDate retiredDate,
			String oyakataNameKr,
			String oyakataNameJp,
			String photoUrl
	) {
		this.externalApiId = externalApiId;
		this.shikonaKr = shikonaKr;
		this.shikonaJp = shikonaJp;
		this.shikonaEn = shikonaEn;
		this.name = name;
		this.birthdate = birthdate;
		this.birthplace = birthplace;
		this.nationality = nationality;
		this.height = height;
		this.weight = weight;
		this.heyaEntity = heyaEntity;
		this.currentRank = currentRank;
		this.highestRank = highestRank;
		this.fightingStyle = fightingStyle;
		this.debutDate = debutDate;
		this.isActive = isActive == null || isActive;
		this.retiredDate = retiredDate;
		this.oyakataNameKr = oyakataNameKr;
		this.oyakataNameJp = oyakataNameJp;
		this.photoUrl = photoUrl;
	}

	/**
	 * 관리자 프로필 수정 화면(/admin/rikishi/{id}/edit)에서 호출. 시코나 이력/반즈케는 별도 관리 대상이라
	 * 여기서 건드리지 않는다. currentRank도 마찬가지 이유(그 바쇼 반즈케가 source of truth)로 대상에서 제외 —
	 * 이 메서드가 currentRank를 손대면 반즈케 갱신 없이도 조용히 null로 덮어써질 수 있다.
	 */
	public void updateProfile(
			String shikonaKr,
			String shikonaJp,
			String name,
			LocalDate birthdate,
			String birthplace,
			String nationality,
			BigDecimal height,
			BigDecimal weight,
			HeyaEntity heyaEntity,
			String highestRank,
			String fightingStyle,
			LocalDate debutDate,
			boolean isActive,
			LocalDate retiredDate,
			String oyakataNameKr,
			String oyakataNameJp,
			String photoUrl
	) {
		this.shikonaKr = shikonaKr;
		this.shikonaKrAuto = false; // 관리자가 저장한 이상 검수된 값으로 본다
		this.shikonaJp = shikonaJp;
		this.name = name;
		this.birthdate = birthdate;
		this.birthplace = birthplace;
		this.nationality = nationality;
		this.height = height;
		this.weight = weight;
		this.heyaEntity = heyaEntity;
		this.highestRank = highestRank;
		this.fightingStyle = fightingStyle;
		this.debutDate = debutDate;
		this.isActive = isActive;
		this.retiredDate = retiredDate;
		this.oyakataNameKr = oyakataNameKr;
		this.oyakataNameJp = oyakataNameJp;
		this.photoUrl = photoUrl;
	}

	/** {@code isShikonaKrAuto()} — null(수동/미입력)도 false로. */
	public boolean isShikonaKrAuto() {
		return Boolean.TRUE.equals(shikonaKrAuto);
	}

	/**
	 * 슬라이스 5 자동 채우기: 로마자 음차로 만든 한국어 시코나를 넣고 "자동" 표시를 켠다.
	 * 관리자가 프로필 수정에서 저장하면(updateProfile) 표시가 꺼진다.
	 */
	public void applyAutoShikonaKr(String shikonaKr) {
		this.shikonaKr = shikonaKr;
		this.shikonaKrAuto = true;
	}
}

