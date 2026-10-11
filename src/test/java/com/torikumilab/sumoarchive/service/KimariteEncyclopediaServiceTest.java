package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.domain.dto.KimariteEntryDTO;
import com.torikumilab.sumoarchive.domain.dto.KimariteGroupDTO;
import com.torikumilab.sumoarchive.domain.entity.constant.KimariteCategory;
import com.torikumilab.sumoarchive.util.KimariteDisplayUtil;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class KimariteEncyclopediaServiceTest {

	private final KimariteEncyclopediaService service = new KimariteEncyclopediaService();

	@Test
	void 공식_6대_분류_및_비기_그룹으로_정상_분류된다() {
		List<KimariteGroupDTO> groups = service.getKimariteEncyclopedia();

		assertThat(groups).isNotEmpty();
		assertThat(groups).extracting(KimariteGroupDTO::code)
				.containsExactly(
						KimariteCategory.KIHON.getCode(),
						KimariteCategory.NAGE.getCode(),
						KimariteCategory.KAKE.getCode(),
						KimariteCategory.SORI.getCode(),
						KimariteCategory.HINERI.getCode(),
						KimariteCategory.TOKUSHU.getCode(),
						KimariteCategory.HIGI.getCode()
				);

		int totalEntries = groups.stream()
				.mapToInt(g -> g.entries().size())
				.sum();
		assertThat(totalEntries).isEqualTo(KimariteDisplayUtil.krSuggestions().size());
	}

	@Test
	void 주요_기술_카테고리_매핑_검증() {
		assertThat(KimariteDisplayUtil.categoryOf("요리키리")).isEqualTo(KimariteCategory.KIHON);
		assertThat(KimariteDisplayUtil.categoryOf("오시다시")).isEqualTo(KimariteCategory.KIHON);
		assertThat(KimariteDisplayUtil.categoryOf("우와테나게")).isEqualTo(KimariteCategory.NAGE);
		assertThat(KimariteDisplayUtil.categoryOf("시타테나게")).isEqualTo(KimariteCategory.NAGE);
		assertThat(KimariteDisplayUtil.categoryOf("우치가케")).isEqualTo(KimariteCategory.KAKE);
		assertThat(KimariteDisplayUtil.categoryOf("소토가케")).isEqualTo(KimariteCategory.KAKE);
		assertThat(KimariteDisplayUtil.categoryOf("츠키오토시")).isEqualTo(KimariteCategory.HINERI);
		assertThat(KimariteDisplayUtil.categoryOf("카타스카시")).isEqualTo(KimariteCategory.HINERI);
		assertThat(KimariteDisplayUtil.categoryOf("츠타에조리")).isEqualTo(KimariteCategory.SORI);
		assertThat(KimariteDisplayUtil.categoryOf("하타키코미")).isEqualTo(KimariteCategory.TOKUSHU);
		assertThat(KimariteDisplayUtil.categoryOf("오쿠리다시")).isEqualTo(KimariteCategory.TOKUSHU);
		assertThat(KimariteDisplayUtil.categoryOf("이사미아시")).isEqualTo(KimariteCategory.HIGI);
		assertThat(KimariteDisplayUtil.categoryOf("한소쿠")).isEqualTo(KimariteCategory.HIGI);
	}

	@Test
	void 일본어_한자_및_로마자로_조회시에도_카테고리가_반환된다() {
		assertThat(KimariteDisplayUtil.categoryOf("寄り切り")).isEqualTo(KimariteCategory.KIHON);
		assertThat(KimariteDisplayUtil.categoryOf("yorikiri")).isEqualTo(KimariteCategory.KIHON);
		assertThat(KimariteDisplayUtil.categoryOf("上手投げ")).isEqualTo(KimariteCategory.NAGE);
		assertThat(KimariteDisplayUtil.categoryOf("uwatenage")).isEqualTo(KimariteCategory.NAGE);
	}

	@Test
	void 협회_공식_82수와_분류별_개수가_일치한다() {
		Map<String, Integer> counts = service.getKimariteEncyclopedia().stream()
				.collect(Collectors.toMap(KimariteGroupDTO::code, g -> g.entries().size()));

		assertThat(counts).containsEntry(KimariteCategory.KIHON.getCode(), 7)
				.containsEntry(KimariteCategory.NAGE.getCode(), 13)
				.containsEntry(KimariteCategory.KAKE.getCode(), 18)
				.containsEntry(KimariteCategory.SORI.getCode(), 6)
				.containsEntry(KimariteCategory.HINERI.getCode(), 19)
				.containsEntry(KimariteCategory.TOKUSHU.getCode(), 19);
	}

	@Test
	void 공식_결정기술이_아닌_이름은_사전에_없다() {
		assertThat(KimariteDisplayUtil.krSuggestions()).doesNotContain("하즈오시", "다시나게");
		assertThat(KimariteDisplayUtil.toKr("hazuoshi")).isEqualTo("hazuoshi");
		assertThat(KimariteDisplayUtil.toKr("dashinage")).isEqualTo("dashinage");
	}

	@Test
	void 적재_데이터에_나온_로마자는_전부_한국어로_바뀐다() {
		// 2025~ torikumi.kimarite에 실제로 저장된 값 중 예전에 사전에 없어 로마자가 그대로 노출되던 것들
		List<String> romaji = List.of("harimanage", "sokubiotoshi", "tokkurinage", "ushiromotare",
				"zubuneri", "kotehineri", "osakate", "izori", "ipponzeoi", "chongake", "kawazugake",
				"tsuriotoshi", "nimaigeri", "okuritsuriotoshi", "sotokomata", "tasukizori", "sabaori",
				"koshinage", "waridashi", "okuritsuridashi", "okurigake", "mitokorozeme", "omata",
				"gasshohineri", "kozumatori", "susotori");
		for (String r : romaji) {
			assertThat(KimariteDisplayUtil.toKr(r)).as(r).isIn(KimariteDisplayUtil.krSuggestions());
		}
	}

	@Test
	void 협회_표기가_아닌_일본어도_인식하고_협회_표기로_보여준다() {
		assertThat(KimariteDisplayUtil.toKr("うっちゃり")).isEqualTo("웃차리");
		assertThat(KimariteDisplayUtil.toJp("うっちゃり")).isEqualTo("打っ棄り");
		assertThat(KimariteDisplayUtil.toJp("utchari")).isEqualTo("打っ棄り");
		assertThat(KimariteDisplayUtil.toJp("肩透かし")).isEqualTo("肩すかし");
	}

	@Test
	void 각_카테고리_내부의_항목은_한국어_가나다순으로_정렬된다() {
		List<KimariteGroupDTO> groups = service.getKimariteEncyclopedia();

		for (KimariteGroupDTO group : groups) {
			List<String> names = group.entries().stream().map(KimariteEntryDTO::kimariteKr).toList();
			List<String> sorted = names.stream().sorted().toList();
			assertThat(names).isEqualTo(sorted);
		}
	}

	@Test
	void 등록된_기술_전체에_한국어_및_일본어_설명이_존재한다() {
		List<KimariteGroupDTO> groups = service.getKimariteEncyclopedia();

		for (KimariteGroupDTO group : groups) {
			for (KimariteEntryDTO entry : group.entries()) {
				assertThat(entry.descriptionKr())
						.as("한국어 설명 누락: " + entry.kimariteKr())
						.isNotBlank();
				assertThat(entry.descriptionJp())
						.as("일본어 설명 누락: " + entry.kimariteKr())
						.isNotBlank();
				assertThat(entry.description())
						.as("호환용 description() 메서드 검증")
						.isEqualTo(entry.descriptionKr());
			}
		}
	}
}
