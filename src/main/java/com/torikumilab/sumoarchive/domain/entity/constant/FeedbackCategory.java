package com.torikumilab.sumoarchive.domain.entity.constant;

/** 의견 보내기 종류. 화면 문구는 messages의 feedback.category.{name} */
public enum FeedbackCategory {
	BUG,        // 오류 (화면이 깨짐, 안 열림)
	DATA,       // 데이터가 틀림 (전적, 이름, 출신지 등)
	SUGGESTION, // 제안
	OTHER       // 기타
}
