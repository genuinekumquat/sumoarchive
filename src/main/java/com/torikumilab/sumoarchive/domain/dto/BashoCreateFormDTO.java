package com.torikumilab.sumoarchive.domain.dto;

import com.torikumilab.sumoarchive.domain.entity.constant.BashoMonth;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 관리자 새 바쇼 생성 화면(/admin/basho/new)의 폼 바인딩 객체.
 * <input type="date">는 ISO(yyyy-MM-dd)만 인식하므로 날짜 필드에 @DateTimeFormat을 명시한다.
 */
@Getter
@Setter
public class BashoCreateFormDTO {

	private Integer bashoYear;
	private BashoMonth bashoMonth;

	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate startDate;

	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate endDate;
}
