package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.domain.dto.KimariteEntryDTO;
import com.torikumilab.sumoarchive.domain.dto.KimariteGroupDTO;
import com.torikumilab.sumoarchive.util.HangulIndexUtil;
import com.torikumilab.sumoarchive.util.KimariteDisplayUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 메인 페이지 "키마리테" 탭(결정기술 백과사전) UI 틀. KimariteDisplayUtil에 이미 있는
 * 한/일 매핑을 그대로 목록화만 한다 - 기술 설명(description)은 아직 콘텐츠가 없어서
 * 전부 null로 내려가고, 화면에서는 "설명 준비 중"으로 대체 표시한다.
 */
@Service
public class KimariteEncyclopediaService {

	public List<KimariteGroupDTO> getKimariteEncyclopedia() {
		List<KimariteEntryDTO> entries = KimariteDisplayUtil.krSuggestions().stream()
				.map(kr -> new KimariteEntryDTO(kr, KimariteDisplayUtil.toJp(kr), null))
				.sorted(Comparator.comparing(KimariteEntryDTO::kimariteKr))
				.toList();

		Map<String, List<KimariteEntryDTO>> byInitial = new LinkedHashMap<>();
		for (KimariteEntryDTO e : entries) {
			byInitial.computeIfAbsent(HangulIndexUtil.indexOf(e.kimariteKr()), k -> new ArrayList<>()).add(e);
		}

		return byInitial.entrySet().stream()
				.map(entry -> new KimariteGroupDTO(entry.getKey(), entry.getValue()))
				.toList();
	}
}
