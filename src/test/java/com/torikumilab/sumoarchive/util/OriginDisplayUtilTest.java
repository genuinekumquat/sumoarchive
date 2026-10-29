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
		assertThat(OriginDisplayUtil.toDetailOriginKr(null, "Mongolia, Töv")).isEqualTo("투브");
		assertThat(OriginDisplayUtil.toDetailOriginKr(null, "Mongolia, Gobi-Altai")).isEqualTo("고비알타이");
		// 출신지가 바뀐 경우 협회 표기(마지막 것)의 지역
		assertThat(OriginDisplayUtil.toDetailOriginKr(null, "Mongolia, Ulaanbaatar - Mongolia, Övörkhangai")).isEqualTo("우브르항가이");
		assertThat(OriginDisplayUtil.toDetailOriginKr(null, "Mongolia, Arkhangai")).isEqualTo("아르항가이");
		assertThat(OriginDisplayUtil.toDetailOriginKr(null, "Russia, Tuva Republic, Kyzyl")).isEqualTo("투바 공화국 크즐");

		// 일본어
		assertThat(OriginDisplayUtil.toDetailOriginJp("Mongolia, Ulaanbaatar")).isEqualTo("ウランバートル");
		assertThat(OriginDisplayUtil.toDetailOriginJp("Kazakhstan, Almaty")).isEqualTo("アルマトイ");
		assertThat(OriginDisplayUtil.toDetailOriginJp("Tokyo-to, Edogawa-ku")).isEqualTo("東京都");
		assertThat(OriginDisplayUtil.toDetailOriginJp("Osaka-fu, Neyagawa-shi")).isEqualTo("大阪府");
		assertThat(OriginDisplayUtil.toDetailOriginJp("Mongolia, Töv")).isEqualTo("トゥブ");
		assertThat(OriginDisplayUtil.toDetailOriginJp("Mongolia, Gobi-Altai")).isEqualTo("ゴビ・アルタイ");
		assertThat(OriginDisplayUtil.toDetailOriginJp("Mongolia, Ulaanbaatar - Mongolia, Sükhbaatar")).isEqualTo("スフバートル");
		assertThat(OriginDisplayUtil.toDetailOriginJp("Mongolia, Ulaanbaatar - Mongolia, Khovd")).isEqualTo("ホブド");
		assertThat(OriginDisplayUtil.toDetailOriginJp("Russia, Tuva Republic, Kyzyl")).isEqualTo("トゥヴァ共和国クズル");
	}

	@Test
	@DisplayName("반즈케 요약용 출신지 (외국은 국가명, 일본은 도도부현)")
	void testToKoreanAndJapaneseSummary() {
		assertThat(OriginDisplayUtil.toKorean("Mongolia, Ulaanbaatar")).isEqualTo("몽골");
		assertThat(OriginDisplayUtil.toJapanese("Mongolia, Ulaanbaatar")).isEqualTo("モンゴル");
		assertThat(OriginDisplayUtil.toKorean("Tokyo-to, Edogawa-ku")).isEqualTo("도쿄도");
		assertThat(OriginDisplayUtil.toJapanese("Tokyo-to, Edogawa-ku")).isEqualTo("東京都");
	}

	@Test
	@DisplayName("출신지가 바뀐 리키시는 마지막(협회 공식) 출신지로 표시")
	void testChangedShusshinUsesLatest() {
		String fujinokawa = "Tokyo-to, Edogawa-ku - Kyoto-fu, Kyoto-shi, Nishikyo-ku";
		assertThat(OriginDisplayUtil.toKorean(fujinokawa)).isEqualTo("교토부");
		assertThat(OriginDisplayUtil.toJapanese(fujinokawa)).isEqualTo("京都府");
		assertThat(OriginDisplayUtil.toDetailOriginKr(null, fujinokawa)).isEqualTo("교토부");
		assertThat(OriginDisplayUtil.toDetailOriginJp(fujinokawa)).isEqualTo("京都府");
		assertThat(OriginDisplayUtil.deriveNationality(fujinokawa)).isEqualTo("일본");

		// 국적이 바뀐 경우 (로가: 몽골 태생, 협회 표기 러시아)
		String roga = "Mongolia, Ulaanbaatar - Russia";
		assertThat(OriginDisplayUtil.toKorean(roga)).isEqualTo("러시아");
		assertThat(OriginDisplayUtil.deriveNationality(roga)).isEqualTo("러시아");
		assertThat(OriginDisplayUtil.deriveNationalityJp(roga)).isEqualTo("ロシア");

		// 세 개 이상이어도 마지막
		assertThat(OriginDisplayUtil.toKorean("Osaka-fu, Ikeda-shi - Okayama-ken, Bizen-shi - Osaka-fu, Toyonaka-shi"))
				.isEqualTo("오사카부");

		// 관리자 직접 입력이 있으면 그대로 우선
		assertThat(OriginDisplayUtil.toDetailOriginKr("도쿄도", fujinokawa)).isEqualTo("도쿄도");
	}
}
