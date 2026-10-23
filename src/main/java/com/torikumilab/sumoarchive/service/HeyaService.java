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
import java.util.HashMap;
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

	/**
	 * 반즈케가 있는 최신 바쇼 기준 헤야별 소속 인원. 여기 없는 헤야 = 지금 소속 선수가 없는 헤야
	 * (폐업했거나, 이름이 바뀌어 명단 임포트가 새 헤야 행을 만든 경우의 옛 이름). 그런 헤야를 따로 표시해 두지 않고
	 * 매번 반즈케로 판단하므로 관리자가 할 일이 없다. 반즈케가 하나도 없으면 빈 맵.
	 */
	public Map<Integer, Long> currentMemberCounts() {
		return bashoRepository.findLatestWithBanzuke()
				.map(basho -> toCountMap(banzukeRepository.countMembersByHeya(basho.getId())))
				.orElse(Map.of());
	}

	public static Map<Integer, Long> toCountMap(List<Object[]> rows) {
		Map<Integer, Long> counts = new HashMap<>();
		for (Object[] row : rows) {
			counts.put((Integer) row[0], (Long) row[1]);
		}
		return counts;
	}

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
