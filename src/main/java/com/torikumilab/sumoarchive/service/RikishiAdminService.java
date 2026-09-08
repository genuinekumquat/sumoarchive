package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.domain.dto.HeyaOptionDTO;
import com.torikumilab.sumoarchive.domain.dto.RikishiAdminRowDTO;
import com.torikumilab.sumoarchive.domain.dto.RikishiEditFormDTO;
import com.torikumilab.sumoarchive.domain.dto.ShikonaAutofillResultDTO;
import com.torikumilab.sumoarchive.domain.entity.HeyaEntity;
import com.torikumilab.sumoarchive.domain.entity.RikishiEntity;
import com.torikumilab.sumoarchive.repository.HeyaRepository;
import com.torikumilab.sumoarchive.repository.RikishiRepository;
import com.torikumilab.sumoarchive.util.ShikonaKrTransliterator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 관리자 "데이터 수동 갱신" 중 리키시 프로필 수정 부분. 반즈케/시코나 이력/토리쿠미 등
 * 다른 데이터 갱신은 범위 밖(기능명세서 상 별도 항목). currentRank(현재 계급)도 같은 이유로 제외 —
 * 그 바쇼의 반즈케가 source of truth라 프로필 수정과는 별도로 반즈케 갱신 시 정해져야 한다.
 */
@Service
@RequiredArgsConstructor
public class RikishiAdminService {

	private final RikishiRepository rikishiRepository;
	private final HeyaRepository heyaRepository;

	@Transactional(readOnly = true)
	public Page<RikishiAdminRowDTO> list(String keyword, Pageable pageable) {
		Page<RikishiEntity> page;
		if (keyword == null || keyword.isBlank()) {
			page = rikishiRepository.findAll(pageable);
		} else {
			String k = keyword.strip();
			page = rikishiRepository.findByShikonaKrContainingOrShikonaJpContainingOrShikonaEnContaining(k, k, k, pageable);
		}
		return page.map(RikishiAdminService::toRowDto);
	}

	@Transactional(readOnly = true)
	public RikishiEditFormDTO getEditForm(Integer rikishiId) {
		RikishiEntity r = rikishiRepository.findById(rikishiId)
				.orElseThrow(() -> new EntityNotFoundException("리키시를 찾을 수 없습니다. id=" + rikishiId));

		RikishiEditFormDTO form = new RikishiEditFormDTO();
		form.setId(r.getId());
		form.setShikonaKr(r.getShikonaKr());
		form.setShikonaKrAuto(r.isShikonaKrAuto());
		form.setShikonaJp(r.getShikonaJp());
		form.setName(r.getName());
		form.setBirthdate(r.getBirthdate());
		form.setBirthplace(r.getBirthplace());
		form.setNationality(r.getNationality());
		form.setHeight(r.getHeight());
		form.setWeight(r.getWeight());
		form.setHeyaId(r.getHeyaEntity() != null ? r.getHeyaEntity().getId() : null);
		form.setHighestRank(r.getHighestRank());
		form.setFightingStyle(r.getFightingStyle());
		form.setDebutDate(r.getDebutDate());
		form.setActive(r.isActive());
		form.setRetiredDate(r.getRetiredDate());
		form.setOyakataNameKr(r.getOyakataNameKr());
		form.setOyakataNameJp(r.getOyakataNameJp());
		form.setPhotoUrl(r.getPhotoUrl());
		return form;
	}

	@Transactional
	public void updateProfile(Integer rikishiId, RikishiEditFormDTO form) {
		RikishiEntity r = rikishiRepository.findById(rikishiId)
				.orElseThrow(() -> new EntityNotFoundException("리키시를 찾을 수 없습니다. id=" + rikishiId));

		HeyaEntity heya = form.getHeyaId() != null
				? heyaRepository.findById(form.getHeyaId())
						.orElseThrow(() -> new EntityNotFoundException("헤야를 찾을 수 없습니다. id=" + form.getHeyaId()))
				: null;

		r.updateProfile(
				blankToNull(form.getShikonaKr()),
				blankToNull(form.getShikonaJp()),
				blankToNull(form.getName()),
				form.getBirthdate(),
				blankToNull(form.getBirthplace()),
				blankToNull(form.getNationality()),
				form.getHeight(),
				form.getWeight(),
				heya,
				blankToNull(form.getHighestRank()),
				blankToNull(form.getFightingStyle()),
				form.getDebutDate(),
				form.isActive(),
				form.getRetiredDate(),
				blankToNull(form.getOyakataNameKr()),
				blankToNull(form.getOyakataNameJp()),
				blankToNull(form.getPhotoUrl())
		);
	}

	/** 아직 한국어 시코나가 없는 리키시 수 (목록 화면 자동 채우기 바에 표시). */
	@Transactional(readOnly = true)
	public long countMissingKoreanShikona() {
		return rikishiRepository.countByShikonaKrIsNull();
	}

	/**
	 * 한국어 시코나가 없는 리키시 전부에 대해 로마자({@code shikonaEn}) 음차로 1차값을 채운다.
	 * 채운 값은 "자동" 표시가 켜진 채라 관리자가 목록에서 뱃지를 보고 다듬을 수 있다(하이브리드).
	 */
	@Transactional
	public ShikonaAutofillResultDTO autofillKoreanShikona() {
		List<RikishiEntity> targets = rikishiRepository.findByShikonaKrIsNull();
		int filled = 0;
		List<String> samples = new ArrayList<>();
		List<String> failed = new ArrayList<>();
		for (RikishiEntity r : targets) {
			String kr = ShikonaKrTransliterator.fromRomaji(r.getShikonaEn());
			if (kr == null || kr.isBlank()) {
				failed.add(r.getShikonaEn() != null && !r.getShikonaEn().isBlank()
						? r.getShikonaEn() : ("id=" + r.getId()));
				continue;
			}
			r.applyAutoShikonaKr(kr);
			filled++;
			if (samples.size() < 15) {
				samples.add(r.getShikonaEn() + " → " + kr);
			}
		}
		return new ShikonaAutofillResultDTO(filled, samples, failed);
	}

	@Transactional(readOnly = true)
	public List<HeyaOptionDTO> getHeyaOptions() {
		return heyaRepository.findAll(Sort.by("nameKr")).stream()
				.map(h -> new HeyaOptionDTO(h.getId(), h.getNameKr()))
				.toList();
	}

	private static String blankToNull(String value) {
		return (value == null || value.isBlank()) ? null : value.strip();
	}

	private static RikishiAdminRowDTO toRowDto(RikishiEntity r) {
		return new RikishiAdminRowDTO(
				r.getId(),
				r.getShikonaKr(),
				r.getShikonaJp(),
				r.isShikonaKrAuto(),
				r.getHeyaEntity() != null ? r.getHeyaEntity().getNameKr() : null,
				r.getHighestRank(),
				statusLabel(r)
		);
	}

	private static String statusLabel(RikishiEntity r) {
		if (r.isActive()) {
			return "현역";
		}
		if (r.getOyakataNameKr() != null && !r.getOyakataNameKr().isBlank()) {
			return "오야카타";
		}
		return "은퇴";
	}
}
