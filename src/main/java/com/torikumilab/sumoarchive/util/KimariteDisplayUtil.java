package com.torikumilab.sumoarchive.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 결정기술(키마리테) 한국어 표기 ↔ 일본어 한자 표기 상호 변환.
 *
 * <p>현재 더미데이터(DataSeeder.KIMARITE_POOL)는 한국어 음차 표기("요리키리" 등)로 저장되고,
 * 외부 API 연동 시에는 일본어("寄り切り") 또는 영문이 들어올 수 있어 양쪽 다 대응한다.
 * 매핑에 없는 값은 그대로 통과시킨다(한/일 칸에 같은 문자열이 노출됨).</p>
 */
public final class KimariteDisplayUtil {

	private KimariteDisplayUtil() {
	}

	// key: 한국어 음차, value: 일본어 한자
	private static final Map<String, String> KR_TO_JP = new LinkedHashMap<>();
	private static final Map<String, String> JP_TO_KR = new LinkedHashMap<>();

	private static void put(String kr, String jp) {
		KR_TO_JP.put(kr, jp);
		JP_TO_KR.put(jp, kr);
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
	}

	/** 한국어 표기 반환. 입력이 이미 한국어면 그대로, 일본어면 매핑된 한국어, 매핑 없으면 원본. */
	public static String toKr(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		if (KR_TO_JP.containsKey(raw)) {
			return raw;
		}
		return JP_TO_KR.getOrDefault(raw, raw);
	}

	/** 일본어 한자 표기 반환. 입력이 이미 일본어면 그대로, 한국어면 매핑된 일본어, 매핑 없으면 원본. */
	public static String toJp(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		if (JP_TO_KR.containsKey(raw)) {
			return raw;
		}
		return KR_TO_JP.getOrDefault(raw, raw);
	}
}
