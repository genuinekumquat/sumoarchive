package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.domain.dto.HeyaDivisionDTO;
import com.torikumilab.sumoarchive.domain.dto.HeyaMemberDTO;
import com.torikumilab.sumoarchive.domain.dto.HeyaPageDTO;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 헤야 페이지(/heya/{id}). 소속 선수는 rikishi.is_active 대신 최신 반즈케에 오른 선수로 정한다 -
 * 지위를 같이 보여줘야 하고, 반즈케에 있으면 현역이라는 게 가장 확실한 기준이라서.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HeyaService {

	private final HeyaRepository heyaRepository;
	private final BashoRepository bashoRepository;
	private final BanzukeRepository banzukeRepository;

	public HeyaPageDTO getHeyaPage(Integer heyaId) {
		HeyaEntity heya = heyaRepository.findById(heyaId)
				.orElseThrow(() -> new EntityNotFoundException("헤야를 찾을 수 없습니다. id=" + heyaId));

		BashoEntity basho = bashoRepository.findLatestWithBanzuke().orElse(null);
		if (basho == null) {
			return new HeyaPageDTO(heya.getId(), heya.getNameKr(), heya.getNameJp(), heya.getIchimonKr(), heya.getIchimonJp(),
					null, null, null, List.of());
		}

		// 쿼리가 계급 순으로 정렬해 주므로 LinkedHashMap에 담으면 마쿠우치→조노구치 순서가 유지된다.
		Map<Division, List<HeyaMemberDTO>> byDivision = new LinkedHashMap<>();
		for (BanzukeEntity b : banzukeRepository.findByBashoAndHeyaOrdered(basho.getId(), heyaId)) {
			RikishiEntity r = b.getRikishiEntity();
			byDivision.computeIfAbsent(b.getDivision(), k -> new ArrayList<>()).add(new HeyaMemberDTO(
					r.getId(),
					r.getShikonaKr() != null ? r.getShikonaKr() : (r.getShikonaJp() != null ? r.getShikonaJp() : r.getShikonaEn()),
					r.getShikonaJp(),
					RankDisplayUtil.rankDisplayKorean(b.getRankName(), b.getRankValue()),
					RankDisplayUtil.rankDisplay(b.getRankName(), b.getRankValue()),
					OriginDisplayUtil.toKorean(r.getOriginKr(), r.getBirthplace()),
					OriginDisplayUtil.toJapanese(r.getBirthplace())
			));
		}

		List<HeyaDivisionDTO> divisions = byDivision.entrySet().stream()
				.map(e -> new HeyaDivisionDTO(e.getKey().name().toLowerCase(), e.getValue()))
				.toList();
		return new HeyaPageDTO(heya.getId(), heya.getNameKr(), heya.getNameJp(), heya.getIchimonKr(), heya.getIchimonJp(),
				basho.getBashoYear(), basho.getBashoMonth().getDisplayNameKr(), basho.getBashoMonth().getDisplayNameJp(),
				divisions);
	}
}
