package com.torikumilab.sumoarchive.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ShikonaKrTransliteratorTest {

	@Test
	void か_た행은_위치_무관_격음() {
		// 한국 스모 팬 실사용 표기: 어두에서도 카/타 (국어원 어두 평음 미적용)
		assertThat(ShikonaKrTransliterator.fromRomaji("Kotozakura")).isEqualTo("코토자쿠라");
		assertThat(ShikonaKrTransliterator.fromRomaji("Terunofuji")).isEqualTo("테루노후지");
		assertThat(ShikonaKrTransliterator.fromRomaji("Takayasu")).isEqualTo("타카야스");
		assertThat(ShikonaKrTransliterator.fromRomaji("Kirishima")).isEqualTo("키리시마");
	}

	@Test
	void 요음과_장음() {
		assertThat(ShikonaKrTransliterator.fromRomaji("Hoshoryu")).isEqualTo("호쇼류");
		assertThat(ShikonaKrTransliterator.fromRomaji("Takanosho")).isEqualTo("타카노쇼");
		assertThat(ShikonaKrTransliterator.fromRomaji("Onosato")).isEqualTo("오노사토");
	}

	@Test
	void ん은_받침_ㄴ() {
		assertThat(ShikonaKrTransliterator.fromRomaji("Endo")).isEqualTo("엔도");
		assertThat(ShikonaKrTransliterator.fromRomaji("Kinbozan")).isEqualTo("킨보잔");
		assertThat(ShikonaKrTransliterator.fromRomaji("Shonannoumi")).isEqualTo("쇼난노우미");
	}

	@Test
	void 모음시작_이름() {
		assertThat(ShikonaKrTransliterator.fromRomaji("Asanoyama")).isEqualTo("아사노야마");
		assertThat(ShikonaKrTransliterator.fromRomaji("Ichiyamamoto")).isEqualTo("이치야마모토");
		assertThat(ShikonaKrTransliterator.fromRomaji("Atamifuji")).isEqualTo("아타미후지");
	}

	@Test
	void 빈값_null() {
		assertThat(ShikonaKrTransliterator.fromRomaji(null)).isNull();
		assertThat(ShikonaKrTransliterator.fromRomaji("  ")).isNull();
		assertThat(ShikonaKrTransliterator.fromRomaji("123")).isNull();
	}
}
