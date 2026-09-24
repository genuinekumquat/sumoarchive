package com.torikumilab.sumoarchive.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 리키시 출신지(birthplace) 및 국적(nationality) 한국어/일본어 변환 유틸.
 *
 * <p>원문은 "&lt;지역&gt;, &lt;나머지&gt;" 꼴이다. 지역이 일본 도도부현(접미사 -ken/-to/-fu 또는
 * 접미사 없는 홋카이도/현 이름)이면 일본 출신(국적: 일본)으로 분류하고,
 * 그 외에는 국가명으로 보아 국적을 해당 국가("몽골", "우크라이나" 등)로 도출한다.</p>
 */
public final class OriginDisplayUtil {

	private OriginDisplayUtil() {
	}

	// key: 접미사(-ken/-to/-fu)를 뗀 도도부현 기본형 또는 sumo-api 원문 국가명(영문)
	private static final Map<String, String> KR_LABEL = new HashMap<>();
	private static final Map<String, String> JP_LABEL = new HashMap<>();
	// 한국어 표기 → 영문 기본형 역매핑 (관리자가 birthplace를 이미 한글로 고쳐둔 경우 사용)
	private static final Map<String, String> KR_TO_BASE = new HashMap<>();

	private static final Set<String> PREFECTURES = new HashSet<>();
	private static final Set<String> COUNTRIES = new HashSet<>();
	private static final Map<String, String> CITY_KR = new HashMap<>();
	private static final Map<String, String> CITY_JP = new HashMap<>();

	private static void pref(String base, String kr, String jp) {
		KR_LABEL.put(base, kr);
		JP_LABEL.put(base, jp);
		KR_TO_BASE.put(kr, base);
		PREFECTURES.add(base);
	}

	private static void country(String en, String kr, String jp) {
		KR_LABEL.put(en, kr);
		JP_LABEL.put(en, jp);
		KR_TO_BASE.put(kr, en);
		COUNTRIES.add(en);
	}

	private static void city(String en, String kr, String jp) {
		CITY_KR.put(en, kr);
		CITY_JP.put(en, jp);
	}

	static {
		// 도도부현(都道府県) 47개 전체
		pref("Hokkaido", "홋카이도", "北海道");
		pref("Aomori", "아오모리현", "青森県");
		pref("Iwate", "이와테현", "岩手県");
		pref("Miyagi", "미야기현", "宮城県");
		pref("Akita", "아키타현", "秋田県");
		pref("Yamagata", "야마가타현", "山形県");
		pref("Fukushima", "후쿠시마현", "福島県");
		pref("Ibaraki", "이바라키현", "茨城県");
		pref("Tochigi", "도치기현", "栃木県");
		pref("Gunma", "군마현", "群馬県");
		pref("Saitama", "사이타마현", "埼玉県");
		pref("Chiba", "지바현", "千葉県");
		pref("Tokyo", "도쿄도", "東京都");
		pref("Kanagawa", "가나가와현", "神奈川県");
		pref("Niigata", "니가타현", "新潟県");
		pref("Toyama", "도야마현", "富山県");
		pref("Ishikawa", "이시카와현", "石川県");
		pref("Fukui", "후쿠이현", "福井県");
		pref("Yamanashi", "야마나시현", "山梨県");
		pref("Nagano", "나가노현", "長野県");
		pref("Gifu", "기후현", "岐阜県");
		pref("Shizuoka", "시즈오카현", "静岡県");
		pref("Aichi", "아이치현", "愛知県");
		pref("Mie", "미에현", "三重県");
		pref("Shiga", "시가현", "滋賀県");
		pref("Kyoto", "교토부", "京都府");
		pref("Osaka", "오사카부", "大阪府");
		pref("Hyogo", "효고현", "兵庫県");
		pref("Nara", "나라현", "奈良県");
		pref("Wakayama", "와카야마현", "和歌山県");
		pref("Tottori", "돗토리현", "鳥取県");
		pref("Shimane", "시마네현", "島根県");
		pref("Okayama", "오카야마현", "岡山県");
		pref("Hiroshima", "히로시마현", "広島県");
		pref("Yamaguchi", "야마구치현", "山口県");
		pref("Tokushima", "도쿠시마현", "徳島県");
		pref("Kagawa", "가가와현", "香川県");
		pref("Ehime", "에히메현", "愛媛県");
		pref("Kochi", "고치현", "高知県");
		pref("Fukuoka", "후쿠오카현", "福岡県");
		pref("Saga", "사가현", "佐賀県");
		pref("Nagasaki", "나가사키현", "長崎県");
		pref("Kumamoto", "구마모토현", "熊本県");
		pref("Oita", "오이타현", "大分県");
		pref("Miyazaki", "미야자키현", "宮崎県");
		pref("Kagoshima", "가고시마현", "鹿児島県");
		pref("Okinawa", "오키나와현", "沖縄県");

		// 스모에 자주 등장하는 외국 국적 위주 (DB에 없는 나라는 원문 영문이 그대로 노출됨)
		country("Mongolia", "몽골", "モンゴル");
		country("Kazakhstan", "카자흐스탄", "カザフスタン");
		country("Ukraine", "우크라이나", "ウクライナ");
		country("Russia", "러시아", "ロシア");
		country("China", "중국", "中国");
		country("Philippines", "필리핀", "フィリピン");
		country("America", "미국", "アメリカ");
		country("USA", "미국", "アメリカ");
		country("UK", "영국", "イギリス");
		country("Georgia", "조지아", "ジョージア");
		country("Brazil", "브라질", "ブラジル");
		country("Bulgaria", "불가리아", "ブルガリア");
		country("Egypt", "이집트", "エジプト");
		country("Czech Republic", "체코", "チェコ");
		country("South Korea", "대한민국", "韓国");
		country("Korea", "대한민국", "韓国");
		country("Tonga", "통가", "トンガ");
		country("Samoa", "사모아", "サモア");
		country("Estonia", "에스토니아", "エストニア");
		country("Argentina", "아르헨티나", "アルゼンチン");
		country("Hungary", "헝가리", "ハンガリー");
		country("Indonesia", "인도네시아", "インドネシア");
		country("India", "인도", "インド");
		country("Taiwan", "대만", "台湾");

		// 외국 주요 출신 도시/지역
		city("Ulaanbaatar", "울란바토르", "ウランバートル");
		city("Uvs", "옵스", "オブス");
		city("Ulaangom", "올랑곰", "オラーンゴム");
		city("Almaty", "알마티", "アルマトイ");
		city("Zaporizhia Oblast", "자포리자", "ザポリージャ");
		city("Vinnytsia Oblast", "빈니차", "ヴィンニツァ");
		city("Bayankhongor", "바양홍고르", "バヤンホンゴル");
		city("Inner Mongolia", "내몽골", "内モンゴル");
		city("Province of Laguna", "라구나", "ラグナ");
		city("T?v", "투브", "トゥブ");
	}

	public static String toKorean(String birthplace) {
		return resolve(birthplace, KR_LABEL);
	}

	/**
	 * 관리자가 직접 입력해둔 한국어 표기(curatedKr)가 있으면 그걸 우선 쓰고,
	 * 없으면(null/공백) birthplace로 자동 계산한다.
	 */
	public static String toKorean(String curatedKr, String birthplace) {
		if (curatedKr != null && !curatedKr.isBlank()) {
			return curatedKr.strip();
		}
		return toKorean(birthplace);
	}

	public static String toJapanese(String birthplace) {
		return resolve(birthplace, JP_LABEL);
	}

	/**
	 * 출신지 원문(birthplace)으로부터 올바른 국적(한국어)을 도출한다.
	 * 일본 도도부현이면 "일본", 외국이면 해당 국가의 한국어명("몽골", "우크라이나" 등) 반환.
	 */
	public static String deriveNationality(String birthplace) {
		if (birthplace == null || birthplace.isBlank()) {
			return "일본";
		}
		String first = birthplace.strip().split(",", 2)[0].strip();
		if (containsHangul(first)) {
			String base = KR_TO_BASE.get(first);
			if (base != null && COUNTRIES.contains(base)) {
				return KR_LABEL.getOrDefault(base, first);
			}
			return "일본";
		}
		String base = stripSuffix(first);
		if (COUNTRIES.contains(base)) {
			return KR_LABEL.getOrDefault(base, base);
		}
		return "일본";
	}

	/**
	 * 출신지 원문(birthplace)으로부터 올바른 국적(일본어)을 도출한다.
	 * 일본 도도부현이면 "日本", 외국이면 해당 국가의 일본어명("モンゴル", "ウクライナ" 등) 반환.
	 */
	public static String deriveNationalityJp(String birthplace) {
		if (birthplace == null || birthplace.isBlank()) {
			return "日本";
		}
		String first = birthplace.strip().split(",", 2)[0].strip();
		if (containsHangul(first)) {
			String base = KR_TO_BASE.get(first);
			if (base != null && COUNTRIES.contains(base)) {
				return JP_LABEL.getOrDefault(base, first);
			}
			return "日本";
		}
		String base = stripSuffix(first);
		if (COUNTRIES.contains(base)) {
			return JP_LABEL.getOrDefault(base, base);
		}
		return "日本";
	}

	/**
	 * 리키시 상세 프로필용 상세 출신지(한국어).
	 * 외국 선수의 경우 도시/지역이 있으면 "울란바토르", 없으면 국가명 반환.
	 * 일본 선수의 경우 도도부현 표기 반환.
	 */
	public static String toDetailOriginKr(String curatedKr, String birthplace) {
		if (curatedKr != null && !curatedKr.isBlank()) {
			return curatedKr.strip();
		}
		if (birthplace == null || birthplace.isBlank()) {
			return null;
		}
		String[] parts = birthplace.strip().split(",", 2);
		String first = parts[0].strip();
		String base = containsHangul(first) ? KR_TO_BASE.get(first) : stripSuffix(first);
		if (base != null && COUNTRIES.contains(base) && parts.length > 1) {
			String cityPart = parts[1].strip();
			String lastCity = cityPart.contains(",") ? cityPart.substring(cityPart.lastIndexOf(',') + 1).strip() : cityPart;
			if (CITY_KR.containsKey(lastCity)) {
				return CITY_KR.get(lastCity);
			}
			if (CITY_KR.containsKey(cityPart)) {
				return CITY_KR.get(cityPart);
			}
			return cityPart;
		}
		return toKorean(birthplace);
	}

	/**
	 * 리키시 상세 프로필용 상세 출신지(일본어).
	 */
	public static String toDetailOriginJp(String birthplace) {
		if (birthplace == null || birthplace.isBlank()) {
			return null;
		}
		String[] parts = birthplace.strip().split(",", 2);
		String first = parts[0].strip();
		String base = containsHangul(first) ? KR_TO_BASE.get(first) : stripSuffix(first);
		if (base != null && COUNTRIES.contains(base) && parts.length > 1) {
			String cityPart = parts[1].strip();
			String lastCity = cityPart.contains(",") ? cityPart.substring(cityPart.lastIndexOf(',') + 1).strip() : cityPart;
			if (CITY_JP.containsKey(lastCity)) {
				return CITY_JP.get(lastCity);
			}
			if (CITY_JP.containsKey(cityPart)) {
				return CITY_JP.get(cityPart);
			}
			return cityPart;
		}
		return toJapanese(birthplace);
	}

	private static String resolve(String birthplace, Map<String, String> label) {
		if (birthplace == null || birthplace.isBlank()) {
			return null;
		}
		String region = birthplace.strip().split(",", 2)[0].strip();

		String base = containsHangul(region) ? KR_TO_BASE.get(region) : stripSuffix(region);
		if (base == null) {
			return region;
		}

		return label.getOrDefault(base, region);
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
