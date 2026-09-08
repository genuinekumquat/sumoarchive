package com.torikumilab.sumoarchive.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 결정기술(키마리테) 한국어 표기 ↔ 일본어 한자 표기 상호 변환.
 *
 * <p>관리자 수동 입력은 한국어 음차 표기("요리키리" 등)로 저장되고, sumo-api는 로마자 소문자
 * ("yorikiri")로 내려준다(슬라이스 4). 세 표기(한국어·일본어 한자·로마자) 어느 쪽이 들어와도
 * 한국어/일본어 칸을 채울 수 있게 대응한다. 매핑에 없는 값은 그대로 통과시킨다(한/일 칸에 같은
 * 문자열이 노출됨).</p>
 */
public final class KimariteDisplayUtil {

	private KimariteDisplayUtil() {
	}

	// key: 한국어 음차, value: 일본어 한자
	private static final Map<String, String> KR_TO_JP = new LinkedHashMap<>();
	private static final Map<String, String> JP_TO_KR = new LinkedHashMap<>();
	// key: sumo-api 로마자 소문자, value: 일본어 한자 (KR 칸은 한자 → 한국어 매핑으로 이어붙임)
	private static final Map<String, String> ROMAJI_TO_JP = new LinkedHashMap<>();

	private static void put(String kr, String jp) {
		KR_TO_JP.put(kr, jp);
		JP_TO_KR.put(jp, kr);
	}

	private static void romaji(String romaji, String jp) {
		ROMAJI_TO_JP.put(romaji, jp);
	}

	static {
		put("요리키리", "寄り切り");
		put("오시다시", "押し出し");
		put("하타키코미", "叩き込み");
		put("츠키다시", "突き出し");
		put("요리타오시", "寄り倒し");
		put("히키오토시", "引き落とし");
		put("우와테나게", "上手投げ");
		put("시타테나게", "下手投げ");
		put("오시타오시", "押し倒し");
		put("츠리다시", "吊り出し");
		put("카케나게", "掛け投げ");
		put("소토가케", "外掛け");
		put("우치가케", "内掛け");
		put("슷타리", "とったり");
		put("요비모도시", "呼び戻し");
		put("코테나게", "小手投げ");
		put("우와테다시나게", "上手出し投げ");
		put("스쿠이나게", "掬い投げ");
		put("츠키오토시", "突き落とし");
		put("하즈오시", "はず押し");
		put("오쿠리다시", "送り出し");
		put("아비세타오시", "浴びせ倒し");
		put("켓타오시", "けたぐり");
		put("우치무소", "内無双");
		// sumo-api 연동(슬라이스 4)으로 실제 등장 빈도가 확인된 기술들의 한국어 음차 보강.
		put("카타스카시", "肩透かし");
		put("웃차리", "うっちゃり");
		put("오쿠리타오시", "送り倒し");
		put("시타테다시나게", "下手出し投げ");
		put("키리카에시", "切り返し");
		put("키메다시", "極め出し");
		put("키메타오시", "極め倒し");
		put("츠키타오시", "突き倒し");
		put("쿠비나게", "首投げ");
		put("시타테히네리", "下手ひねり");
		put("우와테히네리", "上手ひねり");
		put("카이나히네리", "腕ひねり");
		put("이사미아시", "勇み足");
		put("힛카케", "引っ掛け");
		put("마키오토시", "巻き落とし");
		put("와타시코미", "渡し込み");
		put("아시토리", "足取り");
		put("오쿠리나게", "送り投げ");
		put("다시나게", "出し投げ");
		put("니초나게", "二丁投げ");
		put("스소하라이", "裾払い");
		put("코마타스쿠이", "小股掬い");
		put("케카에시", "蹴返し");
		put("츠키히자", "つき膝");
		put("츠키테", "つき手");
		put("코시쿠다케", "腰砕け");
		put("후미다시", "踏み出し");
		put("츠타에조리", "伝え反り");
		put("한소쿠", "反則");
		put("히키와케", "引き分け");
		put("이타미와케", "痛み分け");

		// sumo-api 로마자 → 일본어 한자. 위 KR 매핑에 한자가 이미 있으면 그 문자열을 그대로 써서
		// toKr가 로마자 → 한자 → 한국어로 이어지게 한다. 나머지는 한자까지만(그럼 KR 칸엔 한자 노출).
		// 사용 빈도 상위 위주로 채웠고, 없는 기술은 원본 로마자가 그대로 통과된다.
		romaji("yorikiri", "寄り切り");
		romaji("oshidashi", "押し出し");
		romaji("hatakikomi", "叩き込み");
		romaji("tsukidashi", "突き出し");
		romaji("yoritaoshi", "寄り倒し");
		romaji("hikiotoshi", "引き落とし");
		romaji("uwatenage", "上手投げ");
		romaji("shitatenage", "下手投げ");
		romaji("oshitaoshi", "押し倒し");
		romaji("tsuridashi", "吊り出し");
		romaji("kakenage", "掛け投げ");
		romaji("sotogake", "外掛け");
		romaji("uchigake", "内掛け");
		romaji("tottari", "とったり");
		romaji("yobimodoshi", "呼び戻し");
		romaji("kotenage", "小手投げ");
		romaji("uwatedashinage", "上手出し投げ");
		romaji("sukuinage", "掬い投げ");
		romaji("tsukiotoshi", "突き落とし");
		romaji("hazuoshi", "はず押し");
		romaji("okuridashi", "送り出し");
		romaji("abisetaoshi", "浴びせ倒し");
		romaji("ketaguri", "けたぐり");
		romaji("uchimuso", "内無双");
		// 신규 (한자까지만)
		romaji("katasukashi", "肩透かし");
		romaji("utchari", "うっちゃり");
		romaji("okuritaoshi", "送り倒し");
		romaji("shitatedashinage", "下手出し投げ");
		romaji("kirikaeshi", "切り返し");
		romaji("kimedashi", "極め出し");
		romaji("kimetaoshi", "極め倒し");
		romaji("tsukitaoshi", "突き倒し");
		romaji("kubinage", "首投げ");
		romaji("shitatehineri", "下手ひねり");
		romaji("uwatehineri", "上手ひねり");
		romaji("kainahineri", "腕ひねり");
		romaji("isamiashi", "勇み足");
		romaji("hikkake", "引っ掛け");
		romaji("makiotoshi", "巻き落とし");
		romaji("watashikomi", "渡し込み");
		romaji("ashitori", "足取り");
		romaji("okurinage", "送り投げ");
		romaji("dashinage", "出し投げ");
		romaji("nichonage", "二丁投げ");
		romaji("susoharai", "裾払い");
		romaji("komatasukui", "小股掬い");
		romaji("kekaeshi", "蹴返し");
		romaji("tsutaezori", "伝え反り");
		romaji("tsukihiza", "つき膝");
		romaji("tsukite", "つき手");
		romaji("koshikudake", "腰砕け");
		romaji("fumidashi", "踏み出し");
		romaji("hansoku", "反則");
		romaji("hikiwake", "引き分け");
		romaji("itamiwake", "痛み分け");
	}

	/**
	 * 관리자 토리쿠미 입력 폼의 결정기술 자동완성(&lt;datalist&gt;)용 한국어 음차 표기 목록.
	 * 입력 순서(자주 쓰는 기술 위주)를 유지한다. 자유 입력은 그대로 허용되고 이건 제안일 뿐이다.
	 */
	public static List<String> krSuggestions() {
		return new ArrayList<>(KR_TO_JP.keySet());
	}

	/**
	 * 한국어 표기 반환. 입력이 이미 한국어면 그대로, 일본어면 매핑된 한국어,
	 * 로마자면 한자로 바꾼 뒤 다시 한국어로(한국어 매핑이 없으면 한자), 매핑이 하나도 없으면 원본.
	 */
	public static String toKr(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		if (KR_TO_JP.containsKey(raw)) {
			return raw;
		}
		if (JP_TO_KR.containsKey(raw)) {
			return JP_TO_KR.get(raw);
		}
		String jp = ROMAJI_TO_JP.get(raw);
		if (jp != null) {
			return JP_TO_KR.getOrDefault(jp, jp);
		}
		return raw;
	}

	/**
	 * 일본어 한자 표기 반환. 입력이 이미 일본어면 그대로, 한국어면 매핑된 일본어,
	 * 로마자면 매핑된 한자, 매핑이 하나도 없으면 원본.
	 */
	public static String toJp(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		if (JP_TO_KR.containsKey(raw)) {
			return raw;
		}
		if (KR_TO_JP.containsKey(raw)) {
			return KR_TO_JP.get(raw);
		}
		return ROMAJI_TO_JP.getOrDefault(raw, raw);
	}
}
