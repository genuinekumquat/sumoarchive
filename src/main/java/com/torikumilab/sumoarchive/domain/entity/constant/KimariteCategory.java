package com.torikumilab.sumoarchive.domain.entity.constant;

/**
 * 일본 스모 협회(日本相撲協会) 공식 결정기술(키마리테) 6대 분류 및 비기(승부결과).
 */
public enum KimariteCategory {
	KIHON("kihon", "기본기", "基本技"),
	NAGE("nage", "던지기", "投げ手"),
	KAKE("kake", "걸기", "掛け手"),
	HINERI("hineri", "비틀기", "捻り手"),
	SORI("sori", "젖히기", "反り手"),
	TOKUSHU("tokushu", "특수기", "特殊技"),
	HIGI("higi", "비기·승부결과", "非技・勝負結果");

	private final String code;
	private final String nameKr;
	private final String nameJp;

	KimariteCategory(String code, String nameKr, String nameJp) {
		this.code = code;
		this.nameKr = nameKr;
		this.nameJp = nameJp;
	}

	public String getCode() {
		return code;
	}

	public String getNameKr() {
		return nameKr;
	}

	public String getNameJp() {
		return nameJp;
	}
}
