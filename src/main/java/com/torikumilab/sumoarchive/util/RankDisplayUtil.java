package com.torikumilab.sumoarchive.util;

import com.torikumilab.sumoarchive.domain.entity.constant.RankName;
import com.torikumilab.sumoarchive.domain.entity.constant.Side;

import java.util.EnumMap;
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
}
