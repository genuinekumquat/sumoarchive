package com.torikumilab.sumoarchive.util;

import com.torikumilab.sumoarchive.domain.entity.constant.KimariteCategory;

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
	// key: 한국어 음차, value: 공식 6대 분류 및 비기 카테고리
	private static final Map<String, KimariteCategory> KR_TO_CATEGORY = new LinkedHashMap<>();

	private static void put(String kr, String jp) {
		KR_TO_JP.put(kr, jp);
		JP_TO_KR.put(jp, kr);
	}

	// 협회 표기 말고 흔히 쓰이는 다른 일본어 표기(うっちゃり 등). 들어오면 인식만 하고 표시는 협회 표기로.
	private static void alias(String jp, String kr) {
		JP_TO_KR.put(jp, kr);
	}

	private static void romaji(String romaji, String jp) {
		ROMAJI_TO_JP.put(romaji, jp);
	}

	private static void cat(KimariteCategory category, String... krNames) {
		for (String kr : krNames) {
			KR_TO_CATEGORY.put(kr, category);
		}
	}

	// 기준: 일본 스모 협회 "決まり手八十二手"(https://www.sumo.or.jp/Kimarite/show/) 82수 + 非技 5개.
	// 일본어 표기도 협회 페이지 그대로(打っ棄り·肩すかし 등). 이 목록 밖의 이름(はず押し처럼
	// 공격 방식을 부르는 말 등)은 결정기술이 아니므로 넣지 않는다.
	// put 순서 = 관리자 자동완성 순서라, 실제 적재 데이터에서 자주 나오는 기술을 앞에 둔다.
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
		put("톳타리", "とったり");
		put("요비모도시", "呼び戻し");
		put("코테나게", "小手投げ");
		put("우와테다시나게", "上手出し投げ");
		put("스쿠이나게", "掬い投げ");
		put("츠키오토시", "突き落とし");
		put("오쿠리다시", "送り出し");
		put("아비세타오시", "浴びせ倒し");
		put("케타구리", "蹴手繰り");
		put("우치무소", "内無双");
		put("카타스카시", "肩すかし");
		put("웃차리", "打っ棄り");
		put("오쿠리타오시", "送り倒し");
		put("시타테다시나게", "下手出し投げ");
		put("키리카에시", "切り返し");
		put("키메다시", "極め出し");
		put("키메타오시", "極め倒し");
		put("츠키타오시", "突き倒し");
		put("쿠비나게", "首投げ");
		put("시타테히네리", "下手捻り");
		put("우와테히네리", "上手捻り");
		put("카이나히네리", "腕捻り");
		put("힛카케", "引っ掛け");
		put("마키오토시", "巻き落とし");
		put("와타시코미", "渡し込み");
		put("아시토리", "足取り");
		put("오쿠리나게", "送り投げ");
		put("니초나게", "二丁投げ");
		put("스소하라이", "裾払い");
		put("코마타스쿠이", "小股掬い");
		put("케카에시", "蹴返し");
		put("츠타에조리", "伝え反り");
		put("아미우치", "網打ち");
		put("사카톳타리", "逆取ったり");
		put("쿠비히네리", "首捻り");
		put("오쿠리히키오토시", "送り引き落とし");
		put("하리마나게", "波離間投げ");
		put("소쿠비오토시", "素首落とし");
		put("톳쿠리나게", "徳利投げ");
		put("우시로모타레", "後ろもたれ");
		put("즈부네리", "頭捻り");
		put("코테히네리", "小手捻り");
		put("오사카테", "大逆手");
		put("이조리", "居反り");
		put("잇폰제오이", "一本背負い");
		put("촌가케", "ちょん掛け");
		put("카와즈가케", "河津掛け");
		put("츠리오토시", "吊り落とし");
		put("니마이게리", "二枚蹴り");
		put("오쿠리츠리오토시", "送り吊り落とし");
		put("소토코마타", "外小股");
		put("타스키조리", "たすき反り");
		put("사바오리", "鯖折り");
		put("코시나게", "腰投げ");
		put("와리다시", "割り出し");
		put("오쿠리츠리다시", "送り吊り出し");
		put("오쿠리가케", "送り掛け");
		put("미토코로제메", "三所攻め");
		put("오마타", "大股");
		put("갓쇼히네리", "合掌捻り");
		put("코즈마토리", "小褄取り");
		put("스소토리", "裾取り");
		// 2025~ 적재 데이터에는 아직 없는 기술
		put("야구라나게", "櫓投げ");
		put("츠카미나게", "つかみ投げ");
		put("츠마토리", "褄取り");
		put("슈모쿠조리", "撞木反り");
		put("카케조리", "掛け反り");
		put("소토타스키조리", "外たすき反り");
		put("소토무소", "外無双");
		// 非技(勝負結果) 5개
		put("이사미아시", "勇み足");
		put("츠키히자", "つき膝");
		put("츠키테", "つき手");
		put("코시쿠다케", "腰砕け");
		put("후미다시", "踏み出し");
		// 결정기술은 아니지만 승부 기록에 남는 결과
		put("한소쿠", "反則");
		put("히키와케", "引き分け");
		put("이타미와케", "痛み分け");

		alias("けたぐり", "케타구리");
		alias("肩透かし", "카타스카시");
		alias("うっちゃり", "웃차리");
		alias("下手ひねり", "시타테히네리");
		alias("上手ひねり", "우와테히네리");
		alias("腕ひねり", "카이나히네리");
		alias("逆とったり", "사카톳타리");
		alias("首ひねり", "쿠비히네리");
		alias("ずぶねり", "즈부네리");
		alias("小手ひねり", "코테히네리");
		alias("合掌ひねり", "갓쇼히네리");
		alias("襷反り", "타스키조리");
		alias("外襷反り", "소토타스키조리");

		// sumo-api 로마자 → 일본어 한자(위 put의 협회 표기와 같은 문자열이어야 toKr까지 이어진다).
		romaji("tsukidashi", "突き出し");
		romaji("tsukitaoshi", "突き倒し");
		romaji("oshidashi", "押し出し");
		romaji("oshitaoshi", "押し倒し");
		romaji("yorikiri", "寄り切り");
		romaji("yoritaoshi", "寄り倒し");
		romaji("abisetaoshi", "浴びせ倒し");

		romaji("uwatenage", "上手投げ");
		romaji("shitatenage", "下手投げ");
		romaji("kotenage", "小手投げ");
		romaji("sukuinage", "掬い投げ");
		romaji("uwatedashinage", "上手出し投げ");
		romaji("shitatedashinage", "下手出し投げ");
		romaji("koshinage", "腰投げ");
		romaji("kubinage", "首投げ");
		romaji("ipponzeoi", "一本背負い");
		romaji("nichonage", "二丁投げ");
		romaji("yaguranage", "櫓投げ");
		romaji("kakenage", "掛け投げ");
		romaji("tsukaminage", "つかみ投げ");

		romaji("uchigake", "内掛け");
		romaji("sotogake", "外掛け");
		romaji("chongake", "ちょん掛け");
		romaji("kirikaeshi", "切り返し");
		romaji("kawazugake", "河津掛け");
		romaji("kekaeshi", "蹴返し");
		romaji("ketaguri", "蹴手繰り");
		romaji("mitokorozeme", "三所攻め");
		romaji("watashikomi", "渡し込み");
		romaji("nimaigeri", "二枚蹴り");
		romaji("komatasukui", "小股掬い");
		romaji("sotokomata", "外小股");
		romaji("omata", "大股");
		romaji("tsumatori", "褄取り");
		romaji("kozumatori", "小褄取り");
		romaji("ashitori", "足取り");
		romaji("susotori", "裾取り");
		romaji("susoharai", "裾払い");

		romaji("izori", "居反り");
		romaji("shumokuzori", "撞木反り");
		romaji("kakezori", "掛け反り");
		romaji("tasukizori", "たすき反り");
		romaji("sototasukizori", "外たすき反り");
		romaji("tsutaezori", "伝え反り");

		romaji("tsukiotoshi", "突き落とし");
		romaji("makiotoshi", "巻き落とし");
		romaji("tottari", "とったり");
		romaji("sakatottari", "逆取ったり");
		romaji("katasukashi", "肩すかし");
		romaji("sotomuso", "外無双");
		romaji("uchimuso", "内無双");
		romaji("zubuneri", "頭捻り");
		romaji("uwatehineri", "上手捻り");
		romaji("shitatehineri", "下手捻り");
		romaji("amiuchi", "網打ち");
		romaji("sabaori", "鯖折り");
		romaji("harimanage", "波離間投げ");
		romaji("osakate", "大逆手");
		romaji("kainahineri", "腕捻り");
		romaji("gasshohineri", "合掌捻り");
		romaji("tokkurinage", "徳利投げ");
		romaji("kubihineri", "首捻り");
		romaji("kotehineri", "小手捻り");

		romaji("hikiotoshi", "引き落とし");
		romaji("hikkake", "引っ掛け");
		romaji("hatakikomi", "叩き込み");
		romaji("sokubiotoshi", "素首落とし");
		romaji("tsuridashi", "吊り出し");
		romaji("okuritsuridashi", "送り吊り出し");
		romaji("tsuriotoshi", "吊り落とし");
		romaji("okuritsuriotoshi", "送り吊り落とし");
		romaji("okuridashi", "送り出し");
		romaji("okuritaoshi", "送り倒し");
		romaji("okurinage", "送り投げ");
		romaji("okurigake", "送り掛け");
		romaji("okurihikiotoshi", "送り引き落とし");
		romaji("waridashi", "割り出し");
		romaji("utchari", "打っ棄り");
		romaji("kimedashi", "極め出し");
		romaji("kimetaoshi", "極め倒し");
		romaji("ushiromotare", "後ろもたれ");
		romaji("yobimodoshi", "呼び戻し");

		romaji("isamiashi", "勇み足");
		romaji("koshikudake", "腰砕け");
		romaji("tsukite", "つき手");
		romaji("tsukihiza", "つき膝");
		romaji("fumidashi", "踏み出し");
		romaji("hansoku", "反則");
		romaji("hikiwake", "引き分け");
		romaji("itamiwake", "痛み分け");

		// 협회 공식 분류: 基本技 7 + 投げ手 13 + 掛け手 18 + 反り手 6 + 捻り手 19 + 特殊技 19 = 82, 그 밖에 非技
		cat(KimariteCategory.KIHON,
				"츠키다시", "츠키타오시", "오시다시", "오시타오시", "요리키리",
				"요리타오시", "아비세타오시");

		cat(KimariteCategory.NAGE,
				"우와테나게", "시타테나게", "코테나게", "스쿠이나게", "우와테다시나게",
				"시타테다시나게", "코시나게", "쿠비나게", "잇폰제오이", "니초나게",
				"야구라나게", "카케나게", "츠카미나게");

		cat(KimariteCategory.KAKE,
				"우치가케", "소토가케", "촌가케", "키리카에시", "카와즈가케",
				"케카에시", "케타구리", "미토코로제메", "와타시코미", "니마이게리",
				"코마타스쿠이", "소토코마타", "오마타", "츠마토리", "코즈마토리",
				"아시토리", "스소토리", "스소하라이");

		cat(KimariteCategory.SORI,
				"이조리", "슈모쿠조리", "카케조리", "타스키조리", "소토타스키조리",
				"츠타에조리");

		cat(KimariteCategory.HINERI,
				"츠키오토시", "마키오토시", "톳타리", "사카톳타리", "카타스카시",
				"소토무소", "우치무소", "즈부네리", "우와테히네리", "시타테히네리",
				"아미우치", "사바오리", "하리마나게", "오사카테", "카이나히네리",
				"갓쇼히네리", "톳쿠리나게", "쿠비히네리", "코테히네리");

		cat(KimariteCategory.TOKUSHU,
				"히키오토시", "힛카케", "하타키코미", "소쿠비오토시", "츠리다시",
				"오쿠리츠리다시", "츠리오토시", "오쿠리츠리오토시", "오쿠리다시", "오쿠리타오시",
				"오쿠리나게", "오쿠리가케", "오쿠리히키오토시", "와리다시", "웃차리",
				"키메다시", "키메타오시", "우시로모타레", "요비모도시");

		cat(KimariteCategory.HIGI,
				"이사미아시", "코시쿠다케", "츠키테", "츠키히자", "후미다시",
				"한소쿠", "히키와케", "이타미와케");
	}

	/**
	 * 관리자 토리쿠미 입력 폼의 결정기술 자동완성(&lt;datalist&gt;)용 한국어 음차 표기 목록.
	 * 입력 순서(자주 쓰는 기술 위주)를 유지한다. 자유 입력은 그대로 허용되고 이건 제안일 뿐이다.
	 */
	public static List<String> krSuggestions() {
		return new ArrayList<>(KR_TO_JP.keySet());
	}

	/**
	 * 결정기술(한국어·일본어·로마자 무관)의 공식 카테고리를 반환.
	 * 매핑되지 않은 기술은 기본적으로 특수기(TOKUSHU)로 분류.
	 */
	public static KimariteCategory categoryOf(String raw) {
		if (raw == null || raw.isBlank()) {
			return KimariteCategory.TOKUSHU;
		}
		String kr = toKr(raw);
		KimariteCategory cat = KR_TO_CATEGORY.get(kr);
		if (cat != null) {
			return cat;
		}
		return KimariteCategory.TOKUSHU;
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
	 * 일본어 한자 표기 반환. 입력이 일본어면 협회 표기로(별칭 うっちゃり → 打っ棄り), 한국어면 매핑된 일본어,
	 * 로마자면 매핑된 한자, 매핑이 하나도 없으면 원본.
	 */
	public static String toJp(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		if (JP_TO_KR.containsKey(raw)) {
			return KR_TO_JP.get(JP_TO_KR.get(raw));
		}
		if (KR_TO_JP.containsKey(raw)) {
			return KR_TO_JP.get(raw);
		}
		return ROMAJI_TO_JP.getOrDefault(raw, raw);
	}
}
