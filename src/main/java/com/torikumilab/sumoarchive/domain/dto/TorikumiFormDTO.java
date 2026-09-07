package com.torikumilab.sumoarchive.domain.dto;

import com.torikumilab.sumoarchive.domain.entity.constant.Division;
import com.torikumilab.sumoarchive.domain.entity.constant.ResultType;
import lombok.Getter;
import lombok.Setter;

/**
 * 관리자 토리쿠미 추가/수정 공용 폼 바인딩 객체.
 * winnerSide는 "EAST" / "WEST" / "" (미정). extraMatch가 결정전(플레이오프) 여부.
 * FUZEN(부전승패)이면 kimarite는 무시된다.
 */
@Getter
@Setter
public class TorikumiFormDTO {

	private Integer id;              // 편집 폼 렌더용
	private String bashoLabelKr;     // 편집 폼 제목용

	private Integer day;
	private Division division;
	private Integer eastRikishiId;
	private Integer westRikishiId;
	private String winnerSide;
	private ResultType resultType;
	private String kimarite;
	private boolean extraMatch;
	private String youtubeUrl;
	private String descriptionKr;
	private String descriptionJp;
}
