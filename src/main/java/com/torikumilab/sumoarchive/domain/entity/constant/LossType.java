package com.torikumilab.sumoarchive.domain.entity.constant;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 리키시 프로필 "패배 유형" 분석용 그룹 - 진 쪽 시점에서 "어떻게 졌나"로 결정기술을 묶는다.
 * 공식 6대 분류(KimariteCategory)는 押し出し·寄り切り·叩き込み가 전부 기본기 하나로 묶여 약점이 안 보여서 별도로 둔다.
 * kimarite 값은 torikumi.kimarite 저장값(sumo-api 로마자 소문자) 기준. 목록에 없는 기술은 ETC.
 */
public enum LossType {
	OSHI("밀려서 짐", "押され負け",
			"oshidashi", "oshitaoshi", "tsukidashi", "tsukitaoshi"),
	YORI("잡혀서 몰림", "寄られ負け",
			"yorikiri", "yoritaoshi", "abisetaoshi", "kimedashi", "kimetaoshi", "tsuridashi"),
	// 突き落とし는 공식 분류로는 捻り手지만, 진 쪽에서 보면 옆·앞으로 쓰러지는 패배라 여기에 둔다.
	OCHI("앞으로 고꾸라짐", "引き・叩きで落ちる",
			"hatakikomi", "hikiotoshi", "tsukiotoshi", "katasukashi", "hikkake", "makiotoshi", "okurihikiotoshi"),
	NAGE("던져지거나 비틀림", "投げ・捻り",
			"uwatenage", "shitatenage", "sukuinage", "kotenage", "uwatedashinage", "shitatedashinage",
			"kubinage", "kakenage", "tottari", "sakatottari", "uwatehineri", "shitatehineri",
			"kainahineri", "kubihineri", "amiuchi"),
	OKURI("뒤를 잡힘", "後ろを取られる",
			"okuridashi", "okuritaoshi", "okurinage", "okuritsuridashi"),
	ETC("기타", "その他");

	private static final Map<String, LossType> BY_KIMARITE = Stream.of(values())
			.flatMap(t -> t.kimarites.stream().map(k -> Map.entry(k, t)))
			.collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

	private final String nameKr;
	private final String nameJp;
	private final Set<String> kimarites;

	LossType(String nameKr, String nameJp, String... kimarites) {
		this.nameKr = nameKr;
		this.nameJp = nameJp;
		this.kimarites = Set.of(kimarites);
	}

	public String getNameKr() {
		return nameKr;
	}

	public String getNameJp() {
		return nameJp;
	}

	/** 결정기술 저장값 → 패배 유형. 모르는 기술(새로 생긴 표기 등)이나 null은 ETC. */
	public static LossType of(String kimarite) {
		if (kimarite == null) {
			return ETC;
		}
		return BY_KIMARITE.getOrDefault(kimarite.toLowerCase(), ETC);
	}
}
