package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.domain.dto.TorikumiDetailDTO;
import com.torikumilab.sumoarchive.domain.entity.BanzukeEntity;
import com.torikumilab.sumoarchive.domain.entity.BashoEntity;
import com.torikumilab.sumoarchive.domain.entity.RikishiEntity;
import com.torikumilab.sumoarchive.domain.entity.TorikumiEntity;
import com.torikumilab.sumoarchive.domain.entity.constant.Division;
import com.torikumilab.sumoarchive.domain.entity.constant.ResultType;
import com.torikumilab.sumoarchive.repository.BanzukeRepository;
import com.torikumilab.sumoarchive.repository.TorikumiRepository;
import com.torikumilab.sumoarchive.util.KimariteDisplayUtil;
import com.torikumilab.sumoarchive.util.RankDisplayUtil;
import com.torikumilab.sumoarchive.util.YoutubeUrlUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 토리쿠미(경기) 상세 조립. 전체 페이지와 리키시 프로필의 슬라이드 패널이 공유한다.
 * RikishiDetailService와 같은 규칙(계급 표기 = RankDisplayUtil, 바쇼 라벨 포맷 등)을 재사용.
 */
@Service
@RequiredArgsConstructor
public class TorikumiDetailService {

	private final TorikumiRepository torikumiRepository;
	private final BanzukeRepository banzukeRepository;

	@Transactional(readOnly = true)
	public TorikumiDetailDTO getTorikumiDetail(Integer torikumiId) {
		TorikumiEntity t = torikumiRepository.findDetailById(torikumiId)
				.orElseThrow(() -> new EntityNotFoundException("토리쿠미를 찾을 수 없습니다. id=" + torikumiId));

		BashoEntity basho = t.getBashoEntity();
		RikishiEntity east = t.getEastRikishiEntity();
		RikishiEntity west = t.getWestRikishiEntity();

		// 두 선수의 그 바쇼 기준 반즈케를 한 번에 조회 (RikishiDetailService와 동일한 배치 조회)
		Map<Integer, BanzukeEntity> banzukeByRikishiId = banzukeRepository
				.findByBashoEntityIdAndRikishiEntityIdIn(basho.getId(), List.of(east.getId(), west.getId()))
				.stream()
				.collect(Collectors.toMap(b -> b.getRikishiEntity().getId(), b -> b, (a, b) -> a));

		Integer winnerId = t.getWinnerRikishiEntity() != null ? t.getWinnerRikishiEntity().getId() : null;
		boolean decided = winnerId != null;
		boolean fusen = t.getResultType() == ResultType.FUZEN;

		String rawKimarite = t.getKimarite();
		String kimariteKr = fusen ? null : KimariteDisplayUtil.toKr(rawKimarite);
		String kimariteJp = fusen ? null : KimariteDisplayUtil.toJp(rawKimarite);

		return new TorikumiDetailDTO(
				t.getId(),
				bashoTitleKr(basho),
				bashoLabelJp(basho),
				t.getDay(),
				t.getDay() + "일째",
				t.getDay() + "日目",
				divisionLabel(t.getDivision()),

				east.getId(),
				firstToken(east.getShikonaKr()),
				east.getShikonaJp(),
				rankDisplayOf(banzukeByRikishiId.get(east.getId())),
				decided && winnerId.equals(east.getId()),

				west.getId(),
				firstToken(west.getShikonaKr()),
				west.getShikonaJp(),
				rankDisplayOf(banzukeByRikishiId.get(west.getId())),
				decided && winnerId.equals(west.getId()),

				decided,
				fusen,
				kimariteKr,
				kimariteJp,

				YoutubeUrlUtil.toEmbedUrl(t.getYoutubeUrl()),
				t.getYoutubeUrl(),
				t.getDescriptionKr(),
				t.getDescriptionJp()
		);
	}

	private static String rankDisplayOf(BanzukeEntity b) {
		return b == null ? null : RankDisplayUtil.rankDisplay(b.getRankName(), b.getRankValue());
	}

	// RikishiDetailService.toBashoGameLog와 동일 포맷
	private static String bashoLabelJp(BashoEntity basho) {
		return basho.getBashoYear() + "年"
				+ String.format("%02d", basho.getBashoMonth().getMonthValue()) + "月場所";
	}

	private static String bashoTitleKr(BashoEntity basho) {
		return basho.getBashoYear() + "년 "
				+ basho.getBashoMonth().getMonthValue() + "월 "
				+ basho.getBashoMonth().getDisplayNameKr();
	}

	private static String divisionLabel(Division division) {
		if (division == null) {
			return null;
		}
		return switch (division) {
			case Makuuchi -> "마쿠우치";
			case Juryo -> "주료";
			case Makushita -> "마쿠시타";
			case Sandanme -> "산단메";
			case Jonidan -> "조니단";
			case Jonokuchi -> "조노구치";
		};
	}

	// "아사노야마 히로키" 처럼 시코나+이름이 함께 저장된 경우 앞부분(시코나)만.
	private static String firstToken(String value) {
		if (value == null) {
			return null;
		}
		int idx = value.indexOf(' ');
		return idx > 0 ? value.substring(0, idx) : value;
	}
}
