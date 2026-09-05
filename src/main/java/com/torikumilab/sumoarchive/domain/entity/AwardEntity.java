package com.torikumilab.sumoarchive.domain.entity;

import com.torikumilab.sumoarchive.domain.entity.constant.AwardType;
import com.torikumilab.sumoarchive.domain.entity.constant.Division;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "award",
		uniqueConstraints = {
		@UniqueConstraint(
				name = "uq_award",
				columnNames = {"rikishi_id", "basho_id", "division", "award_type"}
		)
		})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AwardEntity {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "rikishi_id", nullable = false)
	private RikishiEntity rikishiEntity;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "basho_id", nullable = false)
	private BashoEntity bashoEntity;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "division", nullable = false)
	private Division division;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "award_type", nullable = false)
	private AwardType awardType;
	
	@Builder
	private AwardEntity(RikishiEntity rikishiEntity, BashoEntity bashoEntity, Division division, AwardType awardType) {
		this.rikishiEntity = rikishiEntity;
		this.bashoEntity = bashoEntity;
		this.division = division;
		this.awardType = awardType;
	}
}
