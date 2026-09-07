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

	/**
	 * 관리자 반즈케 관리 화면(/admin/basho/{id}/banzuke)에서 한 행의 계급 배치를 수정할 때 호출.
	 * 리키시/바쇼는 행의 정체성이라 바꾸지 않는다(바꿔야 하면 삭제 후 재등록).
	 * rankScore는 시더에서도 항상 null이고 읽는 곳이 없어 대상에서 제외한다.
	 */
	public void updatePlacement(Division division, RankName rankName, Side side, Integer rankValue) {
		this.division = division;
		this.rankName = rankName;
		this.side = side;
		this.rankValue = rankValue;
	}
}
