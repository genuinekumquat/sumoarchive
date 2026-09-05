package com.torikumilab.sumoarchive.domain.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 관리자 리키시 프로필 수정 화면(/admin/rikishi/{id}/edit)의 폼 바인딩 객체.
 * Thymeleaf th:object/th:field로 GET(초기값 채우기)과 POST(제출) 양쪽에 그대로 재사용한다.
 */
@Getter
@Setter
public class RikishiEditFormDTO {

	private Integer id;
	private String shikonaKr;
	private String shikonaJp;
	private String name;

	// <input type="date">는 반드시 ISO(yyyy-MM-dd) 형식만 인식한다.
	// 명시하지 않으면 요청 로케일(한국어 short style, 예: "94. 3. 1.")로 포맷되어 브라우저가 값을 못 읽는다.
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate birthdate;
	private String birthplace;
	private String nationality;
	private BigDecimal height;
	private BigDecimal weight;
	private Integer heyaId; // null이면 무소속
	private String currentRank;
	private String highestRank;
	private String fightingStyle;
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate debutDate;
	private boolean active;
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate retiredDate;
	private String oyakataNameKr;
	private String oyakataNameJp;
	private String photoUrl;
}
