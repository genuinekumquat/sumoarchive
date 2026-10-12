package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.domain.dto.RikishiBookmarkCardDTO;
import com.torikumilab.sumoarchive.domain.entity.BanzukeEntity;
import com.torikumilab.sumoarchive.domain.entity.RikishiEntity;
import com.torikumilab.sumoarchive.domain.entity.ScheduledBoutEntity;
import com.torikumilab.sumoarchive.util.RankDisplayUtil;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 예정 대진(scheduled_bout)에서 "다음 경기"를 고르는 공용 함수 - 즐겨찾기(RikishiBookmarkService)와
 * 리키시 프로필(RikishiDetailService)이 같이 쓴다.
 */
final class NextBouts {

	private NextBouts() {
	}

	/** 일차 오름차순 예정 대진에서 이 리키시의 가장 이른 경기. 상대 계급은 같은 바쇼 반즈케(없으면 null). */
	static RikishiBookmarkCardDTO.NextBout first(Integer rikishiId, List<ScheduledBoutEntity> scheduled,
												  Map<Integer, BanzukeEntity> banzukeByRikishiId) {
		for (ScheduledBoutEntity s : scheduled) {
			RikishiEntity opp;
			if (rikishiId.equals(s.getEastRikishiEntity().getId())) {
				opp = s.getWestRikishiEntity();
			} else if (rikishiId.equals(s.getWestRikishiEntity().getId())) {
				opp = s.getEastRikishiEntity();
			} else {
				continue;
			}
			BanzukeEntity ob = banzukeByRikishiId.get(opp.getId());
			return new RikishiBookmarkCardDTO.NextBout(
					s.getDay(),
					firstToken(opp.getShikonaKr() != null ? opp.getShikonaKr() : opp.getShikonaJp()),
					firstToken(opp.getShikonaJp() != null ? opp.getShikonaJp() : opp.getShikonaKr()),
					ob == null ? null : withSide(RankDisplayUtil.sideDisplayKorean(ob.getSide()),
							RankDisplayUtil.rankDisplayKorean(ob.getRankName(), ob.getRankValue())),
					ob == null ? null : withSide(RankDisplayUtil.sideDisplay(ob.getSide()),
							RankDisplayUtil.rankDisplay(ob.getRankName(), ob.getRankValue())));
		}
		return null;
	}

	/** 예정 대진에 나오는 상대 id들 (상대 계급 조회용) */
	static Set<Integer> opponentIds(List<ScheduledBoutEntity> scheduled, Collection<Integer> ids) {
		return scheduled.stream()
				.flatMap(s -> List.of(s.getEastRikishiEntity().getId(), s.getWestRikishiEntity().getId()).stream())
				.filter(id -> !ids.contains(id))
				.collect(Collectors.toSet());
	}

	static Map<Integer, BanzukeEntity> byRikishiId(List<BanzukeEntity> rows) {
		return rows.stream().collect(Collectors.toMap(b -> b.getRikishiEntity().getId(), Function.identity(), (a, b) -> a));
	}

	/** "아사노야마 히로키" / "朝乃山　広暉"처럼 이름이 붙어 있으면 시코나만 */
	static String firstToken(String name) {
		return name == null ? null : name.strip().split("[\\s　]+")[0];
	}

	static String withSide(String side, String rank) {
		if (rank == null) return side;
		return side == null ? rank : side + " " + rank;
	}
}
