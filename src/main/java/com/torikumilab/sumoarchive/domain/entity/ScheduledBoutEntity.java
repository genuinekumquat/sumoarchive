package com.torikumilab.sumoarchive.domain.entity;

import com.torikumilab.sumoarchive.domain.entity.constant.Division;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 발표됐지만 아직 안 치른 대진 (V6). 결과가 나오면 TorikumiImportService가 torikumi에 넣고 여기선 지운다.
 * 통계·호시토리는 torikumi만 보므로 이 테이블은 "다음 경기" 표시에만 쓴다.
 */
@Entity
@Table(name = "scheduled_bout")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduledBoutEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "basho_id", nullable = false)
	private BashoEntity bashoEntity;

	@Column(name = "day", nullable = false)
	private Integer day;

	@Enumerated(EnumType.STRING)
	@Column(name = "division", nullable = false)
	private Division division;

	@Column(name = "match_no")
	private Integer matchNo;

	@Column(name = "external_id", nullable = false, unique = true)
	private String externalId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "east_rikishi_id", nullable = false)
	private RikishiEntity eastRikishiEntity;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "west_rikishi_id", nullable = false)
	private RikishiEntity westRikishiEntity;

	@UpdateTimestamp
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@Builder
	private ScheduledBoutEntity(BashoEntity bashoEntity, Integer day, Division division, Integer matchNo,
								String externalId, RikishiEntity eastRikishiEntity, RikishiEntity westRikishiEntity) {
		this.bashoEntity = bashoEntity;
		this.day = day;
		this.division = division;
		this.matchNo = matchNo;
		this.externalId = externalId;
		this.eastRikishiEntity = eastRikishiEntity;
		this.westRikishiEntity = westRikishiEntity;
	}

	/** 같은 대진이 다시 들어오면 일차·선수만 맞춘다 (발표 후 바뀌는 경우 대비) */
	public void update(Integer day, Integer matchNo, RikishiEntity east, RikishiEntity west) {
		this.day = day;
		this.matchNo = matchNo;
		this.eastRikishiEntity = east;
		this.westRikishiEntity = west;
	}
}
