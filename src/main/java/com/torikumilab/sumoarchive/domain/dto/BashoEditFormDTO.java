package com.torikumilab.sumoarchive.domain.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 관리자 바쇼 수정 화면(/admin/basho/{id}/edit)의 폼 바인딩 객체.
 * year/month/monthLabelKr는 화면 표시 전용(읽기전용)이고, 실제로 수정하는 건 시작일·종료일뿐이다.
 */
@Getter
@Setter
public class BashoEditFormDTO {

	private Integer id;
	private int year;
	private int month;
	private String monthLabelKr;

	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate startDate;

	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate endDate;
}
