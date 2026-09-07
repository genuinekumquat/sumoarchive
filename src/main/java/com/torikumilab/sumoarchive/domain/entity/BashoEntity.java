package com.torikumilab.sumoarchive.domain.entity;

import com.torikumilab.sumoarchive.domain.entity.constant.BashoMonth;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity // JPA 엔티티임을 명시
@Table(name = "basho", // 매핑될 테이블 이름 지정.
		uniqueConstraints = {
				@UniqueConstraint(
						name = "uq_basho",
						columnNames = {"basho_year", "basho_month"}
				)
		})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BashoEntity {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	
	@Column(name = "basho_year", nullable = false)
	private Integer bashoYear;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "basho_month", nullable = false, length = 3)
	private BashoMonth bashoMonth;
	
	@Column(name = "start_date")
	private LocalDate startDate;

	@Column(name = "end_date")
	private LocalDate endDate;

	// sumo-api의 바쇼 식별자 "YYYYMM" (예: "202607"). 임포트 매칭 키.
	@Column(name = "external_basho_id", length = 6, unique = true)
	private String externalBashoId;

	@Builder   // 생성자 위에 @Builder를 달아주면 이름을 명시하며 객체를 조립할 수 있음
	private BashoEntity(Integer bashoYear, BashoMonth bashoMonth, LocalDate startDate, LocalDate endDate, String externalBashoId) {
		this.bashoYear = bashoYear;
		this.bashoMonth = bashoMonth;
		this.startDate = startDate;
		this.endDate = endDate;
		this.externalBashoId = externalBashoId;
	}

	/**
	 * 관리자 바쇼 수정 화면에서 기간(시작일·종료일)만 갱신한다.
	 * 연도·월은 바쇼의 정체성(uq_basho 키)이라 여기서 바꾸지 않는다 — 잘못 넣었으면 삭제 후 재생성.
	 */
	public void updateSchedule(LocalDate startDate, LocalDate endDate) {
		this.startDate = startDate;
		this.endDate = endDate;
	}

	/** sumo-api 바쇼 임포트에서 기간 + 외부 식별자를 갱신 (연도·월은 매칭 키라 그대로). */
	public void updateFromApi(String externalBashoId, LocalDate startDate, LocalDate endDate) {
		this.externalBashoId = externalBashoId;
		this.startDate = startDate;
		this.endDate = endDate;
	}
}


