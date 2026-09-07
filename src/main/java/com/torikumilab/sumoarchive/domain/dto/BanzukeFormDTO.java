package com.torikumilab.sumoarchive.domain.dto;

import com.torikumilab.sumoarchive.domain.entity.constant.Division;
import com.torikumilab.sumoarchive.domain.entity.constant.RankName;
import com.torikumilab.sumoarchive.domain.entity.constant.Side;
import lombok.Getter;
import lombok.Setter;

/**
 * 관리자 반즈케 행 추가/수정 공용 폼 바인딩 객체.
 * rikishiId는 추가할 때만 쓰이고, 수정은 경로의 banzukeId로 대상을 특정한다.
 * rankValue는 요코즈나처럼 번호가 없는 계급일 때 null (빈 <input>은 @InitBinder에서 null 처리).
 */
@Getter
@Setter
public class BanzukeFormDTO {

	private Integer rikishiId;
	private Division division;
	private RankName rankName;
	private Side side;
	private Integer rankValue;
}
