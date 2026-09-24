package com.torikumilab.sumoarchive.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OriginDisplayUtilTest {

	@Test
	@DisplayName("외국 및 일본 리키시 국적 판별 (한국어)")
	void testDeriveNationalityKr() {
		assertThat(OriginDisplayUtil.deriveNationality("Mongolia, Ulaanbaatar")).isEqualTo("몽골");
		assertThat(OriginDisplayUtil.deriveNationality("Kazakhstan, Almaty")).isEqualTo("카자흐스탄");
		assertThat(OriginDisplayUtil.deriveNationality("Ukraine, Zaporizhia Oblast")).isEqualTo("우크라이나");
		assertThat(OriginDisplayUtil.deriveNationality("Tokyo-to, Edogawa-ku")).isEqualTo("일본");
		assertThat(OriginDisplayUtil.deriveNationality("Osaka-fu, Neyagawa-shi")).isEqualTo("일본");
		assertThat(OriginDisplayUtil.deriveNationality("Hokkaido, Sapporo-shi")).isEqualTo("일본");
		assertThat(OriginDisplayUtil.deriveNationality("Aomori-ken, Goshogawara-shi")).isEqualTo("일본");
	}

	@Test
	@DisplayName("외국 및 일본 리키시 국적 판별 (일본어)")
	void testDeriveNationalityJp() {
		assertThat(OriginDisplayUtil.deriveNationalityJp("Mongolia, Ulaanbaatar")).isEqualTo("モンゴル");
		assertThat(OriginDisplayUtil.deriveNationalityJp("Kazakhstan, Almaty")).isEqualTo("カザフスタン");
		assertThat(OriginDisplayUtil.deriveNationalityJp("Ukraine, Zaporizhia Oblast")).isEqualTo("ウクライナ");
		assertThat(OriginDisplayUtil.deriveNationalityJp("Tokyo-to, Edogawa-ku")).isEqualTo("日本");
		assertThat(OriginDisplayUtil.deriveNationalityJp("Hokkaido")).isEqualTo("日本");
	}

	@Test
	@DisplayName("프로필 상세 출신지 표시 (외국은 도시, 일본은 도도부현)")
	void testToDetailOrigin() {
		// 한국어
		assertThat(OriginDisplayUtil.toDetailOriginKr(null, "Mongolia, Ulaanbaatar")).isEqualTo("울란바토르");
		assertThat(OriginDisplayUtil.toDetailOriginKr(null, "Kazakhstan, Almaty")).isEqualTo("알마티");
		assertThat(OriginDisplayUtil.toDetailOriginKr(null, "Tokyo-to, Edogawa-ku")).isEqualTo("도쿄도");
		assertThat(OriginDisplayUtil.toDetailOriginKr(null, "Osaka-fu, Neyagawa-shi")).isEqualTo("오사카부");

		// 일본어
		assertThat(OriginDisplayUtil.toDetailOriginJp("Mongolia, Ulaanbaatar")).isEqualTo("ウランバートル");
		assertThat(OriginDisplayUtil.toDetailOriginJp("Kazakhstan, Almaty")).isEqualTo("アルマトイ");
		assertThat(OriginDisplayUtil.toDetailOriginJp("Tokyo-to, Edogawa-ku")).isEqualTo("東京都");
		assertThat(OriginDisplayUtil.toDetailOriginJp("Osaka-fu, Neyagawa-shi")).isEqualTo("大阪府");
	}

	@Test
	@DisplayName("반즈케 요약용 출신지 (외국은 국가명, 일본은 도도부현)")
	void testToKoreanAndJapaneseSummary() {
		assertThat(OriginDisplayUtil.toKorean("Mongolia, Ulaanbaatar")).isEqualTo("몽골");
		assertThat(OriginDisplayUtil.toJapanese("Mongolia, Ulaanbaatar")).isEqualTo("モンゴル");
		assertThat(OriginDisplayUtil.toKorean("Tokyo-to, Edogawa-ku")).isEqualTo("도쿄도");
		assertThat(OriginDisplayUtil.toJapanese("Tokyo-to, Edogawa-ku")).isEqualTo("東京都");
	}
}
