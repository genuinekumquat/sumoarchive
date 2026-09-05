package com.torikumilab.sumoarchive.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "rikishi_shikona_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RikishiShikonaHistoryEntity {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	
	// 2. 외래키 연관관계 매핑으로 변경
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "rikishi_id", nullable = false)
	private RikishiEntity rikishiEntity;
	
	@Column(name = "shikona_kr", nullable = false, length = 100)
	private String shikonaKr;
	
	@Column(name = "shikona_jp", nullable = false, length = 100)
	private String shikonaJp;
	
	@Column(name = "valid_from")
	private LocalDate validFrom;
	
	@Column(name = "valid_to")
	private LocalDate validTo;
	
	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;
	
	@Builder
	private RikishiShikonaHistoryEntity(RikishiEntity rikishiEntity, String shikonaKr, String shikonaJp, LocalDate validFrom, LocalDate validTo) {
		this.rikishiEntity = rikishiEntity;
		this.shikonaKr = shikonaKr;
		this.shikonaJp = shikonaJp;
		this.validFrom = validFrom;
		this.validTo = validTo;
	}
}
