package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.domain.dto.KimariteEntryDTO;
import com.torikumilab.sumoarchive.domain.dto.KimariteGroupDTO;
import com.torikumilab.sumoarchive.domain.entity.constant.KimariteCategory;
import com.torikumilab.sumoarchive.util.KimariteDisplayUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 메인 페이지 "키마리테" 탭(결정기술 백과사전) 서비스.
 * 일본 스모 협회 공식 6대 분류(기본기·던지기·걸기·비틀기·젖히기·특수기) 및
 * 비기/승부결과 기준으로 그룹화하여 반환한다.
 */
@Service
public class KimariteEncyclopediaService {

	public List<KimariteGroupDTO> getKimariteEncyclopedia() {
		Map<KimariteCategory, List<KimariteEntryDTO>> byCategory = new LinkedHashMap<>();
		for (KimariteCategory cat : KimariteCategory.values()) {
			byCategory.put(cat, new ArrayList<>());
		}

		for (String kr : KimariteDisplayUtil.krSuggestions()) {
			KimariteCategory cat = KimariteDisplayUtil.categoryOf(kr);
			KimariteEntryDTO entry = new KimariteEntryDTO(kr, KimariteDisplayUtil.toJp(kr), null);
			byCategory.get(cat).add(entry);
		}

		List<KimariteGroupDTO> groups = new ArrayList<>();
		for (Map.Entry<KimariteCategory, List<KimariteEntryDTO>> entry : byCategory.entrySet()) {
			List<KimariteEntryDTO> list = entry.getValue();
			if (!list.isEmpty()) {
				list.sort(Comparator.comparing(KimariteEntryDTO::kimariteKr));
				KimariteCategory cat = entry.getKey();
				groups.add(new KimariteGroupDTO(
						cat.getCode(),
						cat.getNameKr(),
						cat.getNameJp(),
						list
				));
			}
		}

		return groups;
	}
}
