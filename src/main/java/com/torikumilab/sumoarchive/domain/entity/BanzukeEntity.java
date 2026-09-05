package com.torikumilab.sumoarchive.domain.entity;

import com.torikumilab.sumoarchive.domain.entity.constant.Division;
import com.torikumilab.sumoarchive.domain.entity.constant.RankName;
import com.torikumilab.sumoarchive.domain.entity.constant.Side;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "banzuke",
		uniqueConstraints = {
		@UniqueConstraint(
				name = "uq_rikishi_basho",
				columnNames = {"rikishi_id", "basho_id"}
			)
		}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BanzukeEntity {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "rikishi_id", nullable = false)
	private RikishiEntity rikishiEntity;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "basho_id", nullable = false)
	private BashoEntity bashoEntity;
	
	// Enum 매핑
	@Enumerated(EnumType.STRING)
	@Column(name = "division", nullable = false)
	private Division division;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "rank_name", nullable = false)
	private RankName rankName;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "side", nullable = false)
	private Side side;
	
	@Column(name = "rank_value")
	private Integer rankValue;
	
	@Column(name = "rank_score")
	private Integer rankScore;
	
	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;
	
	@Builder
	private BanzukeEntity(RikishiEntity rikishiEntity, BashoEntity bashoEntity, Division division, RankName rankName, Side side, Integer rankValue, Integer rankScore) {
		this.rikishiEntity = rikishiEntity;
		this.bashoEntity = bashoEntity;
		this.division = division;
		this.rankName = rankName;
		this.side = side;
		this.rankValue = rankValue;
		this.rankScore = rankScore;
	}
}
