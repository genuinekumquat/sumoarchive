package com.torikumilab.sumoarchive.util;

import com.torikumilab.sumoarchive.domain.entity.constant.RankName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RankDisplayUtilTest {

	@Test
	@DisplayName("순번 1은 필두로 표기 (일본어)")
	void testRankDisplayHittou() {
		assertThat(RankDisplayUtil.rankDisplay(RankName.Maegashira, 1)).isEqualTo("前頭筆頭");
		assertThat(RankDisplayUtil.rankDisplay(RankName.Juryo, 1)).isEqualTo("十両筆頭");
		assertThat(RankDisplayUtil.rankDisplay(RankName.Makushita, 1)).isEqualTo("幕下筆頭");
		assertThat(RankDisplayUtil.rankDisplay(RankName.Maegashira, 10)).isEqualTo("前頭10枚目");
		assertThat(RankDisplayUtil.rankDisplay(RankName.Sekiwake, 1)).isEqualTo("関脇");
	}

	@Test
	@DisplayName("순번 1은 필두로 표기 (한국어)")
	void testRankDisplayKoreanHittou() {
		assertThat(RankDisplayUtil.rankDisplayKorean(RankName.Maegashira, 1)).isEqualTo("마에가시라필두");
		assertThat(RankDisplayUtil.rankDisplayKorean(RankName.Sandanme, 1)).isEqualTo("산단메필두");
		assertThat(RankDisplayUtil.rankDisplayKorean(RankName.Jonidan, 1)).isEqualTo("조니단필두");
		assertThat(RankDisplayUtil.rankDisplayKorean(RankName.Juryo, 2)).isEqualTo("주료2");
		assertThat(RankDisplayUtil.rankDisplayKorean(RankName.Ozeki, 1)).isEqualTo("오제키");
	}
}
