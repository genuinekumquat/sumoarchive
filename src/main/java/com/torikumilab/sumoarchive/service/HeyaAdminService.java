package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.domain.dto.HeyaAdminRowDTO;
import com.torikumilab.sumoarchive.domain.dto.HeyaNameAutofillResultDTO;
import com.torikumilab.sumoarchive.domain.dto.IchimonOptionDTO;
import com.torikumilab.sumoarchive.domain.entity.HeyaEntity;
import com.torikumilab.sumoarchive.repository.HeyaRepository;
import com.torikumilab.sumoarchive.repository.RikishiRepository;
import com.torikumilab.sumoarchive.util.ShikonaKrTransliterator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
	private final RikishiRepository rikishiRepository;
	private final HeyaService heyaService;

	/** 손볼 헤야(새로 생겼거나 이름이 바뀐 헤야)를 맨 위로, 그다음 로마자순. */
	@Transactional(readOnly = true)
	public List<HeyaAdminRowDTO> list() {
		Map<Integer, Long> memberCounts = heyaService.currentMemberCounts();
		List<HeyaEntity> heyas = heyaRepository.findAllByOrderByNameEnAsc();
		Map<Integer, String> nameEnById = new HashMap<>();
		heyas.forEach(h -> nameEnById.put(h.getId(), h.getNameEn()));
		return heyas.stream()
				.map(h -> toRowDto(h, memberCounts.getOrDefault(h.getId(), 0L), nameEnById.get(h.getMergedIntoHeyaId())))
				.sorted(Comparator.comparing((HeyaAdminRowDTO r) -> !r.needsAttention()))
				.toList();
	}

	/** 일문 선택지 - 이미 지정된 일문들(5개 남짓). 새 일문이 생기는 일은 드물어 화면에서 만들지는 않는다. */
	@Transactional(readOnly = true)
	public List<IchimonOptionDTO> ichimonOptions() {
		Map<String, String> jpByKr = new TreeMap<>();
		for (HeyaEntity h : heyaRepository.findAll()) {
			if (h.getIchimonKr() != null) {
				jpByKr.putIfAbsent(h.getIchimonKr(), h.getIchimonJp());
			}
		}
		return jpByKr.entrySet().stream()
				.map(e -> new IchimonOptionDTO(e.getKey(), e.getValue()))
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
	@CacheEvict(value = {"banzuke", "ichimonStructure"}, allEntries = true)
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

	/** 관리자 검수: 한 헤야의 한/일명·소속 일문을 저장하고 "자동" 표시를 끈다. ichimonKr이 비면 일문 미지정. */
	@Transactional
	@CacheEvict(value = {"banzuke", "ichimonStructure"}, allEntries = true)
	public void saveNames(Integer heyaId, String nameKr, String nameJp, String ichimonKr) {
		HeyaEntity h = heyaRepository.findById(heyaId)
				.orElseThrow(() -> new EntityNotFoundException("헤야를 찾을 수 없습니다. id=" + heyaId));
		String kr = blankToNull(nameKr);
		String jp = blankToNull(nameJp);
		if (kr == null || jp == null) {
			throw new IllegalArgumentException("한국어명과 일본어명은 비울 수 없습니다.");
		}
		String ichimon = blankToNull(ichimonKr);
		if (ichimon == null) {
			h.updateIchimon(null, null);
		} else {
			IchimonOptionDTO option = ichimonOptions().stream()
					.filter(o -> o.ichimonKr().equals(ichimon))
					.findFirst()
					.orElseThrow(() -> new IllegalArgumentException("없는 일문입니다: " + ichimon));
			h.updateIchimon(option.ichimonKr(), option.ichimonJp());
		}
		h.updateNames(kr, jp);
	}

	/**
	 * 헤야 합치기(개명·흡수): source 소속 선수를 전부 target으로 옮기고 source를 target에 합쳐진 것으로 표시한다.
	 * 이후 로스터 임포트가 source의 영문명을 받아도 target으로 넣는다(RosterImportService#resolveMerged) -
	 * sumo-api가 개명을 늦게·일부만 반영하는 경우(예: Tokiwayama → Minatogawa) 대비. source에 합쳐져 있던 헤야도 target으로 다시 잇는다.
	 *
	 * @return 옮긴 리키시 수
	 */
	@Transactional
	@CacheEvict(value = {"banzuke", "ichimonStructure"}, allEntries = true)
	public int merge(Integer sourceId, Integer targetId) {
		if (sourceId.equals(targetId)) {
			throw new IllegalArgumentException("같은 헤야로는 합칠 수 없습니다.");
		}
		HeyaEntity source = findHeya(sourceId);
		HeyaEntity target = findHeya(targetId);
		if (target.getMergedIntoHeyaId() != null) {
			throw new IllegalArgumentException("이미 다른 헤야에 합쳐진 헤야로는 합칠 수 없습니다: " + target.getNameEn());
		}
		for (HeyaEntity h : heyaRepository.findAll()) {
			if (sourceId.equals(h.getMergedIntoHeyaId())) {
				h.mergeInto(targetId);
			}
		}
		source.mergeInto(targetId);
		heyaRepository.flush(); // 아래 벌크 UPDATE가 영속성 컨텍스트를 비우므로 헤야 변경을 먼저 반영
		return rikishiRepository.moveHeya(source, target);
	}

	/** 합치기 해제 - 옮긴 선수는 그대로 두고, 다음 로스터 임포트부터 이 헤야 영문명을 다시 이 헤야로 넣는다. */
	@Transactional
	@CacheEvict(value = {"banzuke", "ichimonStructure"}, allEntries = true)
	public void unmerge(Integer heyaId) {
		findHeya(heyaId).mergeInto(null);
	}

	private HeyaEntity findHeya(Integer heyaId) {
		return heyaRepository.findById(heyaId)
				.orElseThrow(() -> new EntityNotFoundException("헤야를 찾을 수 없습니다. id=" + heyaId));
	}

	private static String blankToNull(String value) {
		return (value == null || value.isBlank()) ? null : value.strip();
	}

	private static HeyaAdminRowDTO toRowDto(HeyaEntity h, long memberCount, String mergedIntoNameEn) {
		return new HeyaAdminRowDTO(
				h.getId(),
				h.getNameKr(),
				h.getNameJp(),
				h.getNameEn(),
				h.isNameKrAuto(),
				h.getIchimonKr(),
				memberCount,
				h.getMergedIntoHeyaId(),
				mergedIntoNameEn
		);
	}
}
