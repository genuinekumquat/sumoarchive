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
	
	@Builder   // 생성자 위에 @Builder를 달아주면 이름을 명시하며 객체를 조립할 수 있음
	private BashoEntity(Integer bashoYear, BashoMonth bashoMonth, LocalDate startDate, LocalDate endDate) {
		this.bashoYear = bashoYear;
		this.bashoMonth = bashoMonth;
		this.startDate = startDate;
		this.endDate = endDate;
	}
}


