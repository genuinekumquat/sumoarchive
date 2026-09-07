package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.domain.dto.BanzukeDTO;
import com.torikumilab.sumoarchive.domain.dto.BashoOptionDTO;
import com.torikumilab.sumoarchive.domain.entity.BashoEntity;
import com.torikumilab.sumoarchive.domain.entity.constant.Division;
import com.torikumilab.sumoarchive.repository.BanzukeRepository;
import com.torikumilab.sumoarchive.repository.BashoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BanzukeService {

	private final BashoRepository bashoRepository;
	private final BanzukeRepository banzukeRepository;

	/** 메인 화면 기본 노출: 가장 최근 바쇼(시작일 기준)의 해당 디비전 반즈케. */
	public List<BanzukeDTO> getLatestBanzuke(Division division) {
		return getBanzuke(null, division);
	}

	/**
	 * bashoId가 있으면 그 바쇼, 없으면 가장 최근 바쇼의 반즈케를 반환한다.
	 * (메인 화면에서 바쇼 선택 드롭다운으로 이전 바쇼를 조회할 때 bashoId를 넘긴다.)
	 */
	public List<BanzukeDTO> getBanzuke(Integer bashoId, Division division) {
		BashoEntity basho = (bashoId != null)
				? bashoRepository.findById(bashoId)
						.orElseThrow(() -> new EntityNotFoundException("바쇼를 찾을 수 없습니다. id=" + bashoId))
				: bashoRepository.findTopByOrderByStartDateDesc()
						.orElseThrow(() -> new IllegalStateException("등록된 바쇼가 없습니다."));

		return banzukeRepository.findByBashoAndDivisionOrdered(basho.getId(), division)
				.stream()
				.map(b -> new BanzukeDTO(
						b.getRikishiEntity().getId(),
						b.getRikishiEntity().getShikonaKr(),
						b.getRankName().name(),
						b.getSide().name(),
						b.getRankValue(),
						b.getRikishiEntity().isActive()
				))
				.toList();
	}

	/** 메인 화면 바쇼 선택 드롭다운 옵션 (최신 바쇼가 맨 앞). */
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
}
