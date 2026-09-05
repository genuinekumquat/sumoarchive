package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.domain.dto.BanzukeDTO;
import com.torikumilab.sumoarchive.domain.entity.BashoEntity;
import com.torikumilab.sumoarchive.domain.entity.constant.Division;
import com.torikumilab.sumoarchive.repository.BanzukeRepository;
import com.torikumilab.sumoarchive.repository.BashoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BanzukeService {
	
	private final BashoRepository bashoRepository;
	private final BanzukeRepository banzukeRepository;
	
	public List<BanzukeDTO> getLatestBanzuke(Division division) {
		BashoEntity latestBasho = bashoRepository
				.findTopByStartDateLessThanEqualOrderByStartDateDesc(LocalDate.now())
				.orElseThrow(() -> new IllegalStateException("등록된 바쇼가 없습니다."));
		
		return banzukeRepository.findByBashoAndDivisionOrdered(latestBasho.getId(), division)
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
}