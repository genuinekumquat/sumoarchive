package com.torikumilab.sumoarchive.util;

/**
 * 이름 첫 글자의 초성(ㄱ~ㅎ)을 뽑아 가나다순 인덱스 그룹 키로 쓴다.
 * 상대전적(対戦成績) 패널을 ㄱㄴㄷ 인덱스로 접어서 보여줄 때 사용.
 */
public final class HangulIndexUtil {

	private HangulIndexUtil() {
	}

	private static final char[] CHOSEONG = {
			'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ',
			'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
	};

	private static final int HANGUL_BASE = 0xAC00;
	private static final int HANGUL_END = 0xD7A3;
	private static final int CHOSEONG_UNIT = 21 * 28;

	/** 한글 음절이 아닌 이름(로마자/한자 폴백 등)이 묶이는 그룹. */
	public static final String OTHER = "#";

	public static String indexOf(String name) {
		if (name == null || name.isBlank()) {
			return OTHER;
		}
		char c = name.strip().charAt(0);
		if (c < HANGUL_BASE || c > HANGUL_END) {
			return OTHER;
		}
		int choseongIdx = (c - HANGUL_BASE) / CHOSEONG_UNIT;
		return String.valueOf(CHOSEONG[choseongIdx]);
	}
}
