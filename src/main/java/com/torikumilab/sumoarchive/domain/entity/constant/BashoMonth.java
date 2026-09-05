package com.torikumilab.sumoarchive.domain.entity.constant;

public enum BashoMonth {
	JAN(1, "하츠바쇼"),
	MAR(3, "하루바쇼"),
	MAY(5, "나츠바쇼"),
	JUL(7, "나고야바쇼"),
	SEP(9, "아키바쇼"),
	NOV(11, "큐슈바쇼");
	
	private final int monthValue;
	private final String displayNameKr;
	
	BashoMonth(int monthValue, String displayNameKr) {
		this.monthValue = monthValue;
		this.displayNameKr = displayNameKr;
	}
	
	public int getMonthValue() {
		return monthValue;
	}
	
	public String getDisplayNameKr() {
		return displayNameKr;
	}
}