package com.torikumilab.sumoarchive.util;

import java.util.HashMap;
import java.util.Map;

/**
 * 리키시 출신지(birthplace, sumo-api의 shusshin 원문) → 반즈케/프로필에 보여줄 한국어 표기.
 *
 * <p>원문은 "&lt;지역&gt;, &lt;나머지&gt;" 꼴이다. 지역이 일본 도도부현(접미사 -ken/-to/-fu 또는
 * 접미사 없는 홋카이도/현 이름)이면 그 현 이름을, 아니면 외국 국가명으로 보고 한국어 국가명을 반환한다.
 * ⚠ RosterImportService#deriveNationality는 콤마 뒤(마지막 토큰)를 국적으로 잘못 뽑아서 도쿄·오사카·
 * 홋카이도 출신 선수의 nationality 컬럼이 시/구 이름으로 잘못 들어가 있다 — 그래서 저장된 nationality
 * 대신 원문 birthplace를 직접 파싱한다(맨 앞 토큰이 항상 지역/국가).</p>
 *
 * <p>매핑에 없는 지역/국가는 KimariteDisplayUtil과 같은 방침으로 원문 그대로 통과시킨다.
 * birthplace에 이미 한글이 섞여 있으면(관리자가 직접 고쳐둔 값) 그대로 반환한다.</p>
 */
public final class OriginDisplayUtil {

	private OriginDisplayUtil() {
	}

	// key: 접미사(-ken/-to/-fu)를 뗀 도도부현 기본형
	private static final Map<String, String> PREFECTURE_KR = new HashMap<>();
	// key: sumo-api 원문 국가명(영문)
	private static final Map<String, String> COUNTRY_KR = new HashMap<>();

	private static void pref(String base, String kr) {
		PREFECTURE_KR.put(base, kr);
	}

	private static void country(String en, String kr) {
		COUNTRY_KR.put(en, kr);
	}

	static {
		// 도도부현(都道府県) 47개 전체
		pref("Hokkaido", "홋카이도");
		pref("Aomori", "아오모리현");
		pref("Iwate", "이와테현");
		pref("Miyagi", "미야기현");
		pref("Akita", "아키타현");
		pref("Yamagata", "야마가타현");
		pref("Fukushima", "후쿠시마현");
		pref("Ibaraki", "이바라키현");
		pref("Tochigi", "도치기현");
		pref("Gunma", "군마현");
		pref("Saitama", "사이타마현");
		pref("Chiba", "지바현");
		pref("Tokyo", "도쿄도");
		pref("Kanagawa", "가나가와현");
		pref("Niigata", "니가타현");
		pref("Toyama", "도야마현");
		pref("Ishikawa", "이시카와현");
		pref("Fukui", "후쿠이현");
		pref("Yamanashi", "야마나시현");
		pref("Nagano", "나가노현");
		pref("Gifu", "기후현");
		pref("Shizuoka", "시즈오카현");
		pref("Aichi", "아이치현");
		pref("Mie", "미에현");
		pref("Shiga", "시가현");
		pref("Kyoto", "교토부");
		pref("Osaka", "오사카부");
		pref("Hyogo", "효고현");
		pref("Nara", "나라현");
		pref("Wakayama", "와카야마현");
		pref("Tottori", "돗토리현");
		pref("Shimane", "시마네현");
		pref("Okayama", "오카야마현");
		pref("Hiroshima", "히로시마현");
		pref("Yamaguchi", "야마구치현");
		pref("Tokushima", "도쿠시마현");
		pref("Kagawa", "가가와현");
		pref("Ehime", "에히메현");
		pref("Kochi", "고치현");
		pref("Fukuoka", "후쿠오카현");
		pref("Saga", "사가현");
		pref("Nagasaki", "나가사키현");
		pref("Kumamoto", "구마모토현");
		pref("Oita", "오이타현");
		pref("Miyazaki", "미야자키현");
		pref("Kagoshima", "가고시마현");
		pref("Okinawa", "오키나와현");

		// 스모에 자주 등장하는 외국 국적 위주 (DB에 없는 나라는 원문 영문이 그대로 노출됨)
		country("Mongolia", "몽골");
		country("Kazakhstan", "카자흐스탄");
		country("Ukraine", "우크라이나");
		country("Russia", "러시아");
		country("China", "중국");
		country("Philippines", "필리핀");
		country("America", "미국");
		country("USA", "미국");
		country("UK", "영국");
		country("Georgia", "조지아");
		country("Brazil", "브라질");
		country("Bulgaria", "불가리아");
		country("Egypt", "이집트");
		country("Czech Republic", "체코");
		country("South Korea", "대한민국");
		country("Korea", "대한민국");
		country("Tonga", "통가");
		country("Samoa", "사모아");
		country("Estonia", "에스토니아");
		country("Argentina", "아르헨티나");
		country("Hungary", "헝가리");
		country("Indonesia", "인도네시아");
		country("India", "인도");
		country("Taiwan", "대만");
	}

	public static String toKorean(String birthplace) {
		if (birthplace == null || birthplace.isBlank()) {
			return null;
		}
		// 원문이든 관리자가 이미 한글로 고쳐둔 값이든 항상 첫 콤마 앞(지역/국가)만 취한다 -
		// 그래야 "토야마현, 토야마시"처럼 관리자가 시/군까지 적어놓은 값도 다른 선수들과
		// 같은 급(도도부현/국가)으로 표시된다.
		String region = birthplace.strip().split(",", 2)[0].strip();
		if (containsHangul(region)) {
			return region;
		}

		String base = stripSuffix(region);
		String prefKr = PREFECTURE_KR.get(base);
		if (prefKr != null) {
			return prefKr;
		}

		return COUNTRY_KR.getOrDefault(region, region);
	}

	private static String stripSuffix(String region) {
		if (region.endsWith("-ken") || region.endsWith("-to") || region.endsWith("-fu")) {
			return region.substring(0, region.lastIndexOf('-'));
		}
		return region;
	}

	private static boolean containsHangul(String s) {
		return s.chars().anyMatch(c -> c >= 0xAC00 && c <= 0xD7A3);
	}
}
