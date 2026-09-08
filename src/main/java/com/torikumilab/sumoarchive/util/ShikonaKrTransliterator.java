package com.torikumilab.sumoarchive.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * sumo-api 로마자 시코나("Asanoyama")를 한국어 음차("아사노야마")로 변환한다.
 * 슬라이스 5 "한국어 시코나 하이브리드"의 자동 채우기 1차값 생성기 — 완벽하진 않고, 관리자가
 * 목록의 "(자동)" 뱃지를 보고 다듬는 것을 전제로 한다.
 *
 * <p>규칙(헵번식 로마자 기준):</p>
 * <ul>
 *   <li>모라 단위 최장 일치. か·た행은 위치와 무관하게 격음(카키쿠케코 / 타테토) —
 *       국립국어원 표기법의 어두 평음보다 한국 스모 팬들의 실사용 표기("타카야스", "키리시마")를 따른다.</li>
 *   <li>っ(촉음, 자음 중첩)은 앞 음절에 받침 'ㅅ', ん(n)은 받침 'ㄴ'으로 붙인다.</li>
 *   <li>매핑에 없는 조각은 건너뛴다. 결과가 비면 {@code null}(→ 호출부에서 실패 처리).</li>
 * </ul>
 */
public final class ShikonaKrTransliterator {

	private ShikonaKrTransliterator() {
	}

	private static final int HANGUL_BASE = 0xAC00;
	private static final int HANGUL_LAST = 0xD7A3;
	private static final int JONG_COUNT = 28;
	private static final int JONG_N = 4;   // ㄴ
	private static final int JONG_S = 19;  // ㅅ

	/** 모라 → 한글. 키는 소문자, 최장 3글자. */
	private static final Map<String, String> MORA = new LinkedHashMap<>();

	static {
		// 요음(3글자) 먼저 — 최장 일치라 순서 자체는 상관없지만 가독성 위해 묶음
		put("kya", "캬"); put("kyu", "큐"); put("kyo", "쿄");
		put("gya", "갸"); put("gyu", "규"); put("gyo", "교");
		put("sha", "샤"); put("shu", "슈"); put("sho", "쇼");
		put("cha", "차"); put("chu", "추"); put("cho", "초");
		put("ja", "자");  put("ju", "주");  put("jo", "조");
		put("jya", "자"); put("jyu", "주"); put("jyo", "조");
		put("nya", "냐"); put("nyu", "뉴"); put("nyo", "뇨");
		put("hya", "햐"); put("hyu", "휴"); put("hyo", "효");
		put("bya", "뱌"); put("byu", "뷰"); put("byo", "뵤");
		put("pya", "퍄"); put("pyu", "퓨"); put("pyo", "표");
		put("mya", "먀"); put("myu", "뮤"); put("myo", "묘");
		put("rya", "랴"); put("ryu", "류"); put("ryo", "료");
		put("shi", "시"); put("chi", "치"); put("tsu", "츠");

		put("a", "아"); put("i", "이"); put("u", "우"); put("e", "에"); put("o", "오");
		put("ka", "카"); put("ki", "키"); put("ku", "쿠"); put("ke", "케"); put("ko", "코");
		put("ga", "가"); put("gi", "기"); put("gu", "구"); put("ge", "게"); put("go", "고");
		put("sa", "사"); put("su", "스"); put("se", "세"); put("so", "소");
		put("za", "자"); put("ji", "지"); put("zu", "즈"); put("ze", "제"); put("zo", "조");
		put("ta", "타"); put("te", "테"); put("to", "토");
		put("da", "다"); put("di", "디"); put("du", "두"); put("de", "데"); put("do", "도");
		put("na", "나"); put("ni", "니"); put("nu", "누"); put("ne", "네"); put("no", "노");
		put("ha", "하"); put("hi", "히"); put("fu", "후"); put("he", "헤"); put("ho", "호");
		put("ba", "바"); put("bi", "비"); put("bu", "부"); put("be", "베"); put("bo", "보");
		put("pa", "파"); put("pi", "피"); put("pu", "푸"); put("pe", "페"); put("po", "포");
		put("ma", "마"); put("mi", "미"); put("mu", "무"); put("me", "메"); put("mo", "모");
		put("ya", "야"); put("yu", "유"); put("yo", "요");
		put("ra", "라"); put("ri", "리"); put("ru", "루"); put("re", "레"); put("ro", "로");
		put("wa", "와"); put("wo", "오"); put("wi", "위"); put("we", "웨");
		put("fa", "파"); put("fi", "피"); put("fe", "페"); put("fo", "포");
		put("va", "바"); put("vi", "비"); put("vu", "부"); put("ve", "베"); put("vo", "보");
	}

	private static void put(String romaji, String hangul) {
		MORA.put(romaji, hangul);
	}

	public static String fromRomaji(String romaji) {
		if (romaji == null) {
			return null;
		}
		String s = romaji.strip().toLowerCase().replaceAll("[^a-z]", "");
		if (s.isEmpty()) {
			return null;
		}

		StringBuilder out = new StringBuilder();
		int i = 0;
		while (i < s.length()) {
			// 촉음(っ): 자음 중첩(k/s/t/p) 또는 "tc"(っち/っちゃ)
			char c = s.charAt(i);
			if (i + 1 < s.length()) {
				char next = s.charAt(i + 1);
				boolean geminate = (c == next && "kstp".indexOf(c) >= 0)
						|| (c == 't' && next == 'c');
				if (geminate) {
					appendJong(out, JONG_S);
					i++;
					continue;
				}
			}

			String key = matchMora(s, i);
			if (key != null) {
				out.append(MORA.get(key));
				i += key.length();
				continue;
			}

			if (c == 'n') {
				appendJong(out, JONG_N);
				i++;
				continue;
			}

			// 매핑 불가한 조각(자음 잔여 등)은 버린다
			i++;
		}

		return out.length() == 0 ? null : out.toString();
	}

	/** i 위치에서 가장 긴(최대 3) 모라 키를 찾아 반환. 없으면 null. */
	private static String matchMora(String s, int i) {
		for (int len = Math.min(3, s.length() - i); len >= 1; len--) {
			String key = s.substring(i, i + len);
			if (MORA.containsKey(key)) {
				return key;
			}
		}
		return null;
	}

	/** out 마지막 음절에 종성을 합성한다. 합성 불가하면 아무것도 안 한다. */
	private static void appendJong(StringBuilder out, int jong) {
		if (out.length() == 0) {
			return;
		}
		char last = out.charAt(out.length() - 1);
		if (last < HANGUL_BASE || last > HANGUL_LAST) {
			return;
		}
		int idx = last - HANGUL_BASE;
		if (idx % JONG_COUNT != 0) {
			return; // 이미 종성이 있음
		}
		out.setCharAt(out.length() - 1, (char) (last + jong));
	}
}
