package com.torikumilab.sumoarchive.domain.entity.constant;

public enum BashoMonth {
	JAN(1, "하츠바쇼", "初場所"),
	MAR(3, "하루바쇼", "春場所"),
	MAY(5, "나츠바쇼", "夏場所"),
	JUL(7, "나고야바쇼", "名古屋場所"),
	SEP(9, "아키바쇼", "秋場所"),
	NOV(11, "큐슈바쇼", "九州場所");

	private final int monthValue;
	private final String displayNameKr;
	private final String displayNameJp;

	BashoMonth(int monthValue, String displayNameKr, String displayNameJp) {
		this.monthValue = monthValue;
		this.displayNameKr = displayNameKr;
		this.displayNameJp = displayNameJp;
	}

	public int getMonthValue() {
		return monthValue;
	}

	public String getDisplayNameKr() {
		return displayNameKr;
	}

	public String getDisplayNameJp() {
		return displayNameJp;
	}
}