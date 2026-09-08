package com.torikumilab.sumoarchive.util;

import com.torikumilab.sumoarchive.domain.entity.constant.RankName;
import com.torikumilab.sumoarchive.domain.entity.constant.Side;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 반즈케 계급(RankName)/순번(rankValue)을 화면 표기용 문자열로 변환.
 * index.html의 JS RANK_LABEL 매핑과 동일한 규칙을 서버 쪽에도 둔 것.
 */
public final class RankDisplayUtil {

	private RankDisplayUtil() {
	}

	private static final Map<RankName, String> LABEL = new EnumMap<>(RankName.class);

	static {
		LABEL.put(RankName.Yokozuna, "横綱");
		LABEL.put(RankName.Ozeki, "大関");
		LABEL.put(RankName.Sekiwake, "関脇");
		LABEL.put(RankName.Komusubi, "小結");
		LABEL.put(RankName.Maegashira, "前頭");
		LABEL.put(RankName.Juryo, "十両");
		LABEL.put(RankName.Makushita, "幕下");
		LABEL.put(RankName.Sandanme, "三段目");
		LABEL.put(RankName.Jonidan, "序二段");
		LABEL.put(RankName.Jonokuchi, "序ノ口");
	}

	/**
	 * 마에가시라 이하(마에가시라·주료·마쿠시타·산단메·조니단·조노구치)는 순번을 붙여서 "前頭10枚目" 형태로,
	 * 요코즈나~코무스비는 순번 없이 "横綱" 형태로 반환.
	 */
	public static String rankDisplay(RankName rankName, Integer rankValue) {
		if (rankName == null) {
			return null;
		}
		String label = LABEL.getOrDefault(rankName, rankName.name());

		boolean numbered = rankName == RankName.Maegashira
				|| rankName == RankName.Juryo
				|| rankName == RankName.Makushita
				|| rankName == RankName.Sandanme
				|| rankName == RankName.Jonidan
				|| rankName == RankName.Jonokuchi;

		if (numbered && rankValue != null) {
			return label + rankValue + "枚目";
		}
		return label;
	}

	public static String sideDisplay(Side side) {
		if (side == null) {
			return null;
		}
		return side == Side.EAST ? "東" : "西";
	}

	// ===== 자유 문자열 계급값(highest_rank 등) → 한국어 음차 =====

	/** index.html의 RANK_LABEL과 같은 한국어 음차. 계급 이름(영문 enum 철자) → 표기. */
	private static final Map<String, String> KR_LABEL = new LinkedHashMap<>();

	static {
		KR_LABEL.put("Yokozuna", "요코즈나");
		KR_LABEL.put("Ozeki", "오제키");
		KR_LABEL.put("Sekiwake", "세키와케");
		KR_LABEL.put("Komusubi", "코무스비");
		KR_LABEL.put("Maegashira", "마에가시라");
		KR_LABEL.put("Juryo", "주료");
		KR_LABEL.put("Makushita", "마쿠시타");
		KR_LABEL.put("Sandanme", "산단메");
		KR_LABEL.put("Jonidan", "조니단");
		KR_LABEL.put("Jonokuchi", "조노구치");
	}

	/**
	 * 자유 문자열로 저장된 계급값(예: {@code "Ozeki"}, {@code "Maegashira 1"})을 한국어 음차 표기로.
	 * 첫 토큰만 매핑하고 뒤에 숫자 등이 붙어 있으면 그대로 이어 붙인다. 매핑에 없으면
	 * (이미 한글이거나 "오야카타" 등) 원문을 그대로 돌려준다. null/공백도 그대로.
	 */
	public static String toKorean(String stored) {
		if (stored == null || stored.isBlank()) {
			return stored;
		}
		String[] parts = stored.strip().split("\\s+", 2);
		String kr = KR_LABEL.get(parts[0]);
		if (kr == null) {
			return stored;
		}
		return parts.length > 1 ? kr + " " + parts[1] : kr;
	}

	/** 관리자 계급 select용: enum 이름(저장값) → 한국어 라벨. 순서 보존. */
	public static Map<String, String> koreanLabelOptions() {
		return new LinkedHashMap<>(KR_LABEL);
	}
}
