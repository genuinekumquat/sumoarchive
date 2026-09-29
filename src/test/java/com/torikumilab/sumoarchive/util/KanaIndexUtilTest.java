package com.torikumilab.sumoarchive.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class KanaIndexUtilTest {

	@Test
	@DisplayName("로마자 → 히라가나: 요음·촉음·ん·장음 기호·본명 제거")
	void toHiragana() {
		assertThat(KanaIndexUtil.toHiragana("Hoshoryu")).isEqualTo("ほしょりゅ");
		assertThat(KanaIndexUtil.toHiragana("Onosato")).isEqualTo("おのさと");
		assertThat(KanaIndexUtil.toHiragana("Kotonowaka")).isEqualTo("ことのわか");
		assertThat(KanaIndexUtil.toHiragana("Kinbozan")).isEqualTo("きんぼざん");
		assertThat(KanaIndexUtil.toHiragana("Shonannoumi")).isEqualTo("しょなんのうみ");
		assertThat(KanaIndexUtil.toHiragana("Tsurugisho")).isEqualTo("つるぎしょ");
		assertThat(KanaIndexUtil.toHiragana("Hokkaifuji")).isEqualTo("ほっかいふじ");
		assertThat(KanaIndexUtil.toHiragana("Ōsunaarashi")).isEqualTo("おすなあらし");
		assertThat(KanaIndexUtil.toHiragana("Hakuho Sho")).isEqualTo("はくほ");
		assertThat(KanaIndexUtil.toHiragana(null)).isEmpty();
	}

	@Test
	@DisplayName("행 판정 - 탁음·반탁음은 원래 행, 로마자 없으면 #")
	void rowOf() {
		assertThat(KanaIndexUtil.rowOf("Abi")).isEqualTo("あ");
		assertThat(KanaIndexUtil.rowOf("Ura")).isEqualTo("あ");
		assertThat(KanaIndexUtil.rowOf("Gonoyama")).isEqualTo("か");
		assertThat(KanaIndexUtil.rowOf("Jokoryu")).isEqualTo("さ");
		assertThat(KanaIndexUtil.rowOf("Chiyoshoma")).isEqualTo("た");
		assertThat(KanaIndexUtil.rowOf("Daieisho")).isEqualTo("た");
		assertThat(KanaIndexUtil.rowOf("Nishikigi")).isEqualTo("な");
		assertThat(KanaIndexUtil.rowOf("Fujinokawa")).isEqualTo("は");
		assertThat(KanaIndexUtil.rowOf("Bushozan")).isEqualTo("は");
		assertThat(KanaIndexUtil.rowOf("Midorifuji")).isEqualTo("ま");
		assertThat(KanaIndexUtil.rowOf("Yoshinofuji")).isEqualTo("や");
		assertThat(KanaIndexUtil.rowOf("Ryuden")).isEqualTo("ら");
		assertThat(KanaIndexUtil.rowOf("Wakatakakage")).isEqualTo("わ");
		assertThat(KanaIndexUtil.rowOf(null)).isEqualTo(KanaIndexUtil.OTHER);
		assertThat(KanaIndexUtil.rowOf("123")).isEqualTo(KanaIndexUtil.OTHER);
	}

	@Test
	@DisplayName("행 안 정렬은 오십음순 - きた < きり < きん(ん은 맨 뒤), 알파벳순(Kinbozan < Kirishima < Kitanowaka)과 다름")
	void gojuonOrder() {
		List<String> sorted = Stream.of("Kotoshoho", "Kirishima", "Kagayaki", "Kitanowaka", "Kinbozan")
				.sorted(KanaIndexUtil.GOJUON)
				.toList();
		assertThat(sorted).containsExactly("Kagayaki", "Kitanowaka", "Kirishima", "Kinbozan", "Kotoshoho");
	}
}
