package com.torikumilab.sumoarchive.util;

import java.text.Collator;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;

/**
 * 일본어 화면 상대전적의 あかさたな 탭용. 한국어 화면의 HangulIndexUtil(ㄱㄴㄷ)에 대응한다.
 *
 * <p>DB의 한자 시코나(豊昇龍)만으로는 읽는 법을 알 수 없어서, 로마자 시코나(sumo-api shikonaEn, "Hoshoryu")를
 * 히라가나(ほしょりゅ)로 바꿔 행(あ・か・さ…)과 정렬 키로 쓴다. sumo-api 로마자는 장음을 생략하므로(ほうしょうりゅう가 아님)
 * 정렬이 완벽한 오십음순은 아니지만, 행 판정에는 영향이 없다.</p>
 */
public final class KanaIndexUtil {

	private KanaIndexUtil() {
	}

	/** 행 대표 글자 (탭 표시 순서) */
	public static final String[] ROWS = {"あ", "か", "さ", "た", "な", "は", "ま", "や", "ら", "わ"};
	/** 로마자가 없거나 가나로 못 바꾸는 이름 */
	public static final String OTHER = "#";

	private static final Collator JA = Collator.getInstance(Locale.JAPANESE);

	/** 히라가나 기준 오십음 정렬 (행 안의 순서) */
	public static final Comparator<String> GOJUON = (a, b) -> JA.compare(toHiragana(a), toHiragana(b));

	// 헵번식 로마자 → 히라가나. 긴 것부터 맞춰야 해서(shi > si) 길이별로 나눠 찾는다.
	private static final Map<String, String> KANA = Map.ofEntries(
			Map.entry("a", "あ"), Map.entry("i", "い"), Map.entry("u", "う"), Map.entry("e", "え"), Map.entry("o", "お"),
			Map.entry("ka", "か"), Map.entry("ki", "き"), Map.entry("ku", "く"), Map.entry("ke", "け"), Map.entry("ko", "こ"),
			Map.entry("ga", "が"), Map.entry("gi", "ぎ"), Map.entry("gu", "ぐ"), Map.entry("ge", "げ"), Map.entry("go", "ご"),
			Map.entry("sa", "さ"), Map.entry("shi", "し"), Map.entry("si", "し"), Map.entry("su", "す"), Map.entry("se", "せ"), Map.entry("so", "そ"),
			Map.entry("za", "ざ"), Map.entry("ji", "じ"), Map.entry("zi", "じ"), Map.entry("zu", "ず"), Map.entry("ze", "ぜ"), Map.entry("zo", "ぞ"),
			Map.entry("ta", "た"), Map.entry("chi", "ち"), Map.entry("ti", "ち"), Map.entry("tsu", "つ"), Map.entry("tu", "つ"), Map.entry("te", "て"), Map.entry("to", "と"),
			Map.entry("da", "だ"), Map.entry("di", "ぢ"), Map.entry("du", "づ"), Map.entry("de", "で"), Map.entry("do", "ど"),
			Map.entry("na", "な"), Map.entry("ni", "に"), Map.entry("nu", "ぬ"), Map.entry("ne", "ね"), Map.entry("no", "の"),
			Map.entry("ha", "は"), Map.entry("hi", "ひ"), Map.entry("fu", "ふ"), Map.entry("hu", "ふ"), Map.entry("he", "へ"), Map.entry("ho", "ほ"),
			Map.entry("ba", "ば"), Map.entry("bi", "び"), Map.entry("bu", "ぶ"), Map.entry("be", "べ"), Map.entry("bo", "ぼ"),
			Map.entry("pa", "ぱ"), Map.entry("pi", "ぴ"), Map.entry("pu", "ぷ"), Map.entry("pe", "ぺ"), Map.entry("po", "ぽ"),
			Map.entry("ma", "ま"), Map.entry("mi", "み"), Map.entry("mu", "む"), Map.entry("me", "め"), Map.entry("mo", "も"),
			Map.entry("ya", "や"), Map.entry("yu", "ゆ"), Map.entry("yo", "よ"),
			Map.entry("ra", "ら"), Map.entry("ri", "り"), Map.entry("ru", "る"), Map.entry("re", "れ"), Map.entry("ro", "ろ"),
			Map.entry("wa", "わ"), Map.entry("wo", "を"),
			Map.entry("kya", "きゃ"), Map.entry("kyu", "きゅ"), Map.entry("kyo", "きょ"),
			Map.entry("gya", "ぎゃ"), Map.entry("gyu", "ぎゅ"), Map.entry("gyo", "ぎょ"),
			Map.entry("sha", "しゃ"), Map.entry("shu", "しゅ"), Map.entry("sho", "しょ"), Map.entry("she", "しぇ"),
			Map.entry("ja", "じゃ"), Map.entry("ju", "じゅ"), Map.entry("jo", "じょ"), Map.entry("je", "じぇ"),
			Map.entry("cha", "ちゃ"), Map.entry("chu", "ちゅ"), Map.entry("cho", "ちょ"), Map.entry("che", "ちぇ"),
			Map.entry("nya", "にゃ"), Map.entry("nyu", "にゅ"), Map.entry("nyo", "にょ"),
			Map.entry("hya", "ひゃ"), Map.entry("hyu", "ひゅ"), Map.entry("hyo", "ひょ"),
			Map.entry("bya", "びゃ"), Map.entry("byu", "びゅ"), Map.entry("byo", "びょ"),
			Map.entry("pya", "ぴゃ"), Map.entry("pyu", "ぴゅ"), Map.entry("pyo", "ぴょ"),
			Map.entry("mya", "みゃ"), Map.entry("myu", "みゅ"), Map.entry("myo", "みょ"),
			Map.entry("rya", "りゃ"), Map.entry("ryu", "りゅ"), Map.entry("ryo", "りょ")
	);

	/**
	 * "Hoshoryu" → "ほしょりゅ". 모르는 글자가 나오면 그 자리부터는 버린다 (행 판정·정렬용이라 앞부분만 맞으면 충분).
	 * 로마자가 없으면 빈 문자열.
	 */
	public static String toHiragana(String romaji) {
		if (romaji == null) {
			return "";
		}
		// "Ōnosato"의 장음 기호 등 제거, 소문자, 첫 단어(시코나)만
		String s = Normalizer.normalize(romaji.strip(), Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "")
				.toLowerCase(Locale.ROOT)
				.split("[\\s']+", 2)[0];
		StringBuilder out = new StringBuilder();
		int i = 0;
		while (i < s.length()) {
			char c = s.charAt(i);
			// 촉음: 같은 자음이 겹치거나(kk, tt, ss, pp) tch
			if (i + 1 < s.length() && c != 'n' && isConsonant(c)
					&& (s.charAt(i + 1) == c || (c == 't' && s.charAt(i + 1) == 'c'))) {
				out.append('っ');
				i++;
				continue;
			}
			String matched = null;
			for (int len = 3; len >= 1 && matched == null; len--) {
				if (i + len <= s.length()) {
					String kana = KANA.get(s.substring(i, i + len));
					if (kana != null) {
						matched = kana;
						i += len;
					}
				}
			}
			if (matched != null) {
				out.append(matched);
			} else if (c == 'n') {
				// 뒤에 모음·y가 없는 n은 ん (Kotonowaka의 no는 위에서 이미 매칭됨)
				out.append('ん');
				i++;
			} else {
				break;
			}
		}
		return out.toString();
	}

	/** 로마자 이름이 속한 행("あ"~"わ"). 탁음·반탁음은 원래 행으로 (が→か, ぱ→は). */
	public static String rowOf(String romaji) {
		String kana = toHiragana(romaji);
		if (kana.isEmpty()) {
			return OTHER;
		}
		// NFD로 쪼개면 が = か + 탁점이라 첫 글자가 기본 가나가 된다.
		char c = Normalizer.normalize(kana, Normalizer.Form.NFD).charAt(0);
		if (c <= 'お') return "あ";
		if (c <= 'こ') return "か";
		if (c <= 'そ') return "さ";
		if (c <= 'と') return "た";
		if (c <= 'の') return "な";
		if (c <= 'ほ') return "は";
		if (c <= 'も') return "ま";
		if (c <= 'よ') return "や";
		if (c <= 'ろ') return "ら";
		if (c <= 'ん') return "わ";
		return OTHER;
	}

	private static boolean isConsonant(char c) {
		return c >= 'a' && c <= 'z' && "aiueo".indexOf(c) < 0;
	}
}
