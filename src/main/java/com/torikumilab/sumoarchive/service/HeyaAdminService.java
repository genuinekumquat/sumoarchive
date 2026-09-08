package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.domain.dto.HeyaAdminRowDTO;
import com.torikumilab.sumoarchive.domain.dto.HeyaNameAutofillResultDTO;
import com.torikumilab.sumoarchive.domain.entity.HeyaEntity;
import com.torikumilab.sumoarchive.repository.HeyaRepository;
import com.torikumilab.sumoarchive.util.ShikonaKrTransliterator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 관리자 "데이터 수동 갱신" 중 헤야(部屋) 한국어명 보강 부분.
 * sumo-api 로스터 임포트는 헤야를 로마자명("Takasago")만 알고 한/일명은 그 로마자를 그대로
 * 임시로 넣어둔다(NOT NULL 회피). 여기서 로마자 음차로 한국어명 1차값을 만들고
 * (RikishiEntity 시코나와 같은 {@link ShikonaKrTransliterator}), 관리자가 목록에서 다듬는다.
 * 일본어명(한자)은 음차로 만들 수 없어 관리자 수동 입력 대상이다.
 */
@Service
@RequiredArgsConstructor
public class HeyaAdminService {

	private final HeyaRepository heyaRepository;

	@Transactional(readOnly = true)
	public List<HeyaAdminRowDTO> list() {
		return heyaRepository.findAllByOrderByNameEnAsc().stream()
				.map(HeyaAdminService::toRowDto)
				.toList();
	}

	/** 한국어명이 자동 음차값인(검수 전) 헤야 수 (자동 채우기 바에 표시). */
	@Transactional(readOnly = true)
	public long countMissingKoreanName() {
		return heyaRepository.countNameKrAutofillTargets();
	}

	/**
	 * 한국어명이 자동값인 헤야에 대해 로마자({@code nameEn}) 음차로 1차값을 (재)생성한다.
	 * 관리자가 저장해 검수된(nameKrAuto=false) 행은 건드리지 않는다.
	 */
	@Transactional
	public HeyaNameAutofillResultDTO autofillKoreanName() {
		List<HeyaEntity> targets = heyaRepository.findNameKrAutofillTargets();
		int filled = 0;
		List<String> samples = new ArrayList<>();
		List<String> failed = new ArrayList<>();
		for (HeyaEntity h : targets) {
			String kr = ShikonaKrTransliterator.fromRomaji(h.getNameEn());
			if (kr == null || kr.isBlank()) {
				failed.add(h.getNameEn() != null && !h.getNameEn().isBlank()
						? h.getNameEn() : ("id=" + h.getId()));
				continue;
			}
			h.applyAutoNameKr(kr);
			filled++;
			if (samples.size() < 15) {
				samples.add(h.getNameEn() + " → " + kr);
			}
		}
		return new HeyaNameAutofillResultDTO(filled, samples, failed);
	}

	/** 관리자 검수: 한 헤야의 한/일명을 저장하고 "자동" 표시를 끈다. */
	@Transactional
	public void saveNames(Integer heyaId, String nameKr, String nameJp) {
		HeyaEntity h = heyaRepository.findById(heyaId)
				.orElseThrow(() -> new EntityNotFoundException("헤야를 찾을 수 없습니다. id=" + heyaId));
		String kr = blankToNull(nameKr);
		String jp = blankToNull(nameJp);
		if (kr == null || jp == null) {
			throw new IllegalArgumentException("한국어명과 일본어명은 비울 수 없습니다.");
		}
		h.updateNames(kr, jp);
	}

	private static String blankToNull(String value) {
		return (value == null || value.isBlank()) ? null : value.strip();
	}

	private static HeyaAdminRowDTO toRowDto(HeyaEntity h) {
		return new HeyaAdminRowDTO(
				h.getId(),
				h.getNameKr(),
				h.getNameJp(),
				h.getNameEn(),
				h.isNameKrAuto()
		);
	}
}
