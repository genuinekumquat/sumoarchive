package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.domain.dto.BanzukeDTO;
import com.torikumilab.sumoarchive.domain.dto.BashoOptionDTO;
import com.torikumilab.sumoarchive.domain.dto.HeyaSekitoriDTO;
import com.torikumilab.sumoarchive.domain.dto.IchimonGroupDTO;
import com.torikumilab.sumoarchive.domain.dto.SekitoriDTO;
import com.torikumilab.sumoarchive.domain.entity.BanzukeEntity;
import com.torikumilab.sumoarchive.domain.entity.BashoEntity;
import com.torikumilab.sumoarchive.domain.entity.HeyaEntity;
import com.torikumilab.sumoarchive.domain.entity.RikishiEntity;
import com.torikumilab.sumoarchive.domain.entity.constant.Division;
import com.torikumilab.sumoarchive.repository.BanzukeRepository;
import com.torikumilab.sumoarchive.repository.BashoRepository;
import com.torikumilab.sumoarchive.repository.HeyaRepository;
import com.torikumilab.sumoarchive.util.OriginDisplayUtil;
import com.torikumilab.sumoarchive.util.RankDisplayUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BanzukeService {

	private final BashoRepository bashoRepository;
	private final BanzukeRepository banzukeRepository;
	private final HeyaRepository heyaRepository;

	/** 메인 화면 기본 노출: 가장 최근 바쇼(시작일 기준)의 해당 디비전 반즈케. */
	public List<BanzukeDTO> getLatestBanzuke(Division division) {
		return getBanzuke(null, division);
	}

	/**
	 * bashoId가 있으면 그 바쇼, 없으면 가장 최근 바쇼의 반즈케를 반환한다.
	 * (메인 화면에서 바쇼 선택 드롭다운으로 이전 바쇼를 조회할 때 bashoId를 넘긴다.)
	 */
	@Cacheable(value = "banzuke", key = "(#bashoId != null ? #bashoId : 'latest') + '-' + #division.name()")
	public List<BanzukeDTO> getBanzuke(Integer bashoId, Division division) {
		BashoEntity basho;
		if (bashoId != null) {
			basho = bashoRepository.findById(bashoId)
					.orElseThrow(() -> new EntityNotFoundException("바쇼를 찾을 수 없습니다. id=" + bashoId));
		} else {
			// 바쇼가 하나도 없으면(예: sumo-api 임포트 직후, 바쇼 임포트 전) 빈 목록으로 곱게 처리
			basho = bashoRepository.findTopByOrderByStartDateDesc().orElse(null);
			if (basho == null) {
				return List.of();
			}
		}

		return banzukeRepository.findByBashoAndDivisionOrdered(basho.getId(), division)
				.stream()
				.map(b -> new BanzukeDTO(
						b.getRikishiEntity().getId(),
						b.getRikishiEntity().getShikonaKr(),
						b.getRikishiEntity().getShikonaJp(),
						b.getRikishiEntity().getShikonaEn(),
						b.getRankName().name(),
						b.getSide().name(),
						b.getRankValue(),
						b.getRikishiEntity().isActive(),
						b.getRikishiEntity().getHeyaEntity() != null ? b.getRikishiEntity().getHeyaEntity().getNameKr() : null,
						b.getRikishiEntity().getHeyaEntity() != null ? b.getRikishiEntity().getHeyaEntity().getNameJp() : null,
						OriginDisplayUtil.toKorean(b.getRikishiEntity().getOriginKr(), b.getRikishiEntity().getBirthplace()),
						OriginDisplayUtil.toJapanese(b.getRikishiEntity().getBirthplace())
				))
				.toList();
	}

	/** 메인 화면 바쇼 선택 드롭다운 옵션 (최신 바쇼가 맨 앞). */
	@Cacheable(value = "bashoOptions")
	public List<BashoOptionDTO> listBashoOptions() {
		return bashoRepository.findAllByOrderByStartDateDesc().stream()
				.map(b -> new BashoOptionDTO(
						b.getId(),
						b.getBashoYear() + "年 " + b.getBashoMonth().getDisplayNameJp(),
						b.getBashoYear(),
						b.getBashoMonth().getMonthValue(),
						b.getBashoMonth().getDisplayNameJp()
				))
				.toList();
	}

	/**
	 * 메인 페이지 "일문" 탭 - 최신 바쇼 기준 일문(一門)별 헤야, 그 안에 세키토리(마쿠우치+주료) 명단.
	 * 현재 세키토리가 없는 헤야도 목록에는 남는다(전체 헤야 구조를 보여주는 게 목적).
	 * 이치몬이 아직 배정 안 된 헤야는 제외 - 지금은 전부 배정돼 있지만 이후 새 헤야가 생기면 비어있을 수 있음.
	 */
	@Cacheable(value = "ichimonStructure")
	public List<IchimonGroupDTO> getIchimonStructure() {
		BashoEntity basho = bashoRepository.findTopByOrderByStartDateDesc().orElse(null);
		if (basho == null) {
			return List.of();
		}

		Map<Integer, List<SekitoriDTO>> sekitoriByHeyaId = new LinkedHashMap<>();
		for (BanzukeEntity b : banzukeRepository.findSekitoriByBasho(basho.getId())) {
			RikishiEntity r = b.getRikishiEntity();
			if (r.getHeyaEntity() == null) {
				continue; // 무소속(은퇴 예정 등 드문 케이스) - 일문 구조에는 낄 자리가 없어 스킵
			}
			SekitoriDTO dto = new SekitoriDTO(
					r.getId(),
					r.getShikonaKr() != null ? r.getShikonaKr() : (r.getShikonaJp() != null ? r.getShikonaJp() : r.getShikonaEn()),
					r.getShikonaJp(),
					RankDisplayUtil.rankDisplayKorean(b.getRankName(), b.getRankValue()),
					RankDisplayUtil.rankDisplay(b.getRankName(), b.getRankValue())
			);
			sekitoriByHeyaId.computeIfAbsent(r.getHeyaEntity().getId(), k -> new ArrayList<>()).add(dto);
		}

		Map<String, List<HeyaSekitoriDTO>> heyaByIchimonKr = new LinkedHashMap<>();
		Map<String, String> ichimonJpByKr = new LinkedHashMap<>();
		for (HeyaEntity h : heyaRepository.findAllByOrderByNameKrAsc()) {
			if (h.getIchimonKr() == null) {
				continue;
			}
			heyaByIchimonKr.computeIfAbsent(h.getIchimonKr(), k -> new ArrayList<>())
					.add(new HeyaSekitoriDTO(
							h.getId(), h.getNameKr(), h.getNameJp(),
							sekitoriByHeyaId.getOrDefault(h.getId(), List.of())
					));
			ichimonJpByKr.putIfAbsent(h.getIchimonKr(), h.getIchimonJp());
		}

		return heyaByIchimonKr.entrySet().stream()
				.sorted((a, b) -> b.getValue().size() - a.getValue().size()) // 소속 헤야 많은 일문부터
				.map(e -> new IchimonGroupDTO(e.getKey(), ichimonJpByKr.get(e.getKey()), e.getValue()))
				.toList();
	}
}
