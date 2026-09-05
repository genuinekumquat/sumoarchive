package com.torikumilab.sumoarchive.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "kinboshi")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KinboshiEntity {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "torikumi_id", nullable = false)
	private TorikumiEntity torikumiEntity;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "winner_rikishi_id", nullable = false)
	private RikishiEntity winnerRikishiEntity;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "loser_yokozuna_id", nullable = false)
	private RikishiEntity loserYokozuna;
	
	@Builder
	private KinboshiEntity(TorikumiEntity torikumiEntity, RikishiEntity winnerRikishiEntity, RikishiEntity loserYokozuna) {
		this.torikumiEntity = torikumiEntity;
		this.winnerRikishiEntity = winnerRikishiEntity;
		this.loserYokozuna = loserYokozuna;
	}
}
