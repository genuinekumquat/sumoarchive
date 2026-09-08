package com.torikumilab.sumoarchive.domain.entity;

import com.torikumilab.sumoarchive.domain.entity.constant.Division;
import com.torikumilab.sumoarchive.domain.entity.constant.ResultType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "torikumi",
		uniqueConstraints = {
				@UniqueConstraint(
						name = "uq_torikumi",
						columnNames = {"basho_id", "day", "east_rikishi_id", "west_rikishi_id", "is_extra_match"}
				)
		})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TorikumiEntity {
	
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
	
	@Column(name = "external_id", unique = true)
	private String externalId;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "east_rikishi_id", nullable = false)
	private RikishiEntity eastRikishiEntity;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "west_rikishi_id", nullable = false)
	private RikishiEntity westRikishiEntity;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "winner_rikishi_id")
	private RikishiEntity winnerRikishiEntity;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "loser_rikishi_id")
	private RikishiEntity loserRikishiEntity;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "result_type", nullable = false)
	private ResultType resultType;
	
	@Column(name = "kimarite", length = 100)
	private String kimarite;
	
	@Column(name = "is_extra_match",  nullable = false)
	private boolean isExtraMatch;
	
	@Column(name = "youtube_url", length = 255)
	private String youtubeUrl;
	
	@Column(name = "description_kr")
	private String descriptionKr;
	
	@Column(name = "description_jp")
	private String descriptionJp;
	
	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;
	
	@Builder
	private TorikumiEntity(
			BashoEntity bashoEntity,
			Integer day,
			Division division,
			Integer matchNo,
			String externalId,
			RikishiEntity eastRikishiEntity,
			RikishiEntity westRikishiEntity,
			RikishiEntity winnerRikishiEntity,
			RikishiEntity loserRikishiEntity,
			ResultType resultType,
			String kimarite,
			boolean isExtraMatch,
			String youtubeUrl,
			String descriptionKr,
			String descriptionJp
	) {
		this.bashoEntity = bashoEntity;
		this.day = day;
		this.division = division;
		this.matchNo = matchNo;
		this.externalId = externalId;
		this.eastRikishiEntity = eastRikishiEntity;
		this.westRikishiEntity = westRikishiEntity;
		this.winnerRikishiEntity = winnerRikishiEntity;
		this.loserRikishiEntity = loserRikishiEntity;
		
		this.resultType = (resultType == null)
				? ResultType.NORMAL
				: resultType;
		
		this.kimarite = kimarite;

		this.isExtraMatch = isExtraMatch;
		this.youtubeUrl = youtubeUrl;
		this.descriptionKr = descriptionKr;
		this.descriptionJp = descriptionJp;
	}

	/**
	 * 관리자 토리쿠미 편집 화면(/admin/basho/{id}/torikumi/{id}/edit)에서 한 경기를 통째로 갱신한다.
	 * 바쇼는 경로로 고정돼 있어 바꾸지 않는다. externalId/matchNo는 관리자가 다루지 않으므로 유지.
	 */
	public void updateMatch(
			Integer day,
			Division division,
			RikishiEntity eastRikishiEntity,
			RikishiEntity westRikishiEntity,
			RikishiEntity winnerRikishiEntity,
			RikishiEntity loserRikishiEntity,
			ResultType resultType,
			String kimarite,
			boolean isExtraMatch,
			String youtubeUrl,
			String descriptionKr,
			String descriptionJp
	) {
		this.day = day;
		this.division = division;
		this.eastRikishiEntity = eastRikishiEntity;
		this.westRikishiEntity = westRikishiEntity;
		this.winnerRikishiEntity = winnerRikishiEntity;
		this.loserRikishiEntity = loserRikishiEntity;
		this.resultType = (resultType == null) ? ResultType.NORMAL : resultType;
		this.kimarite = kimarite;
		this.isExtraMatch = isExtraMatch;
		this.youtubeUrl = youtubeUrl;
		this.descriptionKr = descriptionKr;
		this.descriptionJp = descriptionJp;
	}

	/**
	 * sumo-api 토리쿠미 임포트 재실행 시 API 소유 필드만 갱신한다.
	 * youtubeUrl / descriptionKr / descriptionJp 는 우리 큐레이션 필드라 건드리지 않는다.
	 * externalId / bashoEntity / isExtraMatch(항상 false로 임포트)는 행의 정체성이라 유지.
	 */
	public void updateFromApi(
			Integer day,
			Division division,
			Integer matchNo,
			RikishiEntity eastRikishiEntity,
			RikishiEntity westRikishiEntity,
			RikishiEntity winnerRikishiEntity,
			RikishiEntity loserRikishiEntity,
			ResultType resultType,
			String kimarite
	) {
		this.day = day;
		this.division = division;
		this.matchNo = matchNo;
		this.eastRikishiEntity = eastRikishiEntity;
		this.westRikishiEntity = westRikishiEntity;
		this.winnerRikishiEntity = winnerRikishiEntity;
		this.loserRikishiEntity = loserRikishiEntity;
		this.resultType = (resultType == null) ? ResultType.NORMAL : resultType;
		this.kimarite = kimarite;
	}
}