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
	
	@Column(name = "shikona_jp", length = 100)
	private String shikonaJp;
	
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
}

