package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.domain.dto.RikishiBookmarkCardDTO;
import com.torikumilab.sumoarchive.domain.entity.RikishiEntity;
import com.torikumilab.sumoarchive.repository.RikishiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 북마크 페이지(/bookmark)용. LocalStorage에 저장된 리키시 ID 배열을 받아 카드 목록을 만든다.
 * 정렬(드래그 앤 드롭 순서)은 클라이언트가 들고 있는 ID 순서를 그대로 존중해야 하므로,
 * 여기서는 요청받은 ids 순서대로 결과를 맞춰 돌려준다.
 */
@Service
@RequiredArgsConstructor
public class RikishiBookmarkService {

	private final RikishiRepository rikishiRepository;

	@Transactional(readOnly = true)
	public List<RikishiBookmarkCardDTO> getBookmarkCards(List<Integer> ids) {
		Map<Integer, RikishiEntity> byId = rikishiRepository.findAllById(ids).stream()
				.collect(Collectors.toMap(RikishiEntity::getId, r -> r));

		// 이미 삭제된 리키시 등 더 이상 존재하지 않는 id는 조용히 스킵한다.
		return ids.stream()
				.map(byId::get)
				.filter(Objects::nonNull)
				.map(RikishiBookmarkService::toDto)
				.toList();
	}

	private static RikishiBookmarkCardDTO toDto(RikishiEntity r) {
		return new RikishiBookmarkCardDTO(
				r.getId(),
				r.getShikonaKr(),
				r.getShikonaJp(),
				r.getHeyaEntity() != null ? r.getHeyaEntity().getNameKr() : null,
				r.getHighestRank(),
				formatPeriod(r.getDebutDate(), r.getRetiredDate()),
				statusLabel(r),
				r.getPhotoUrl()
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

	private static String formatPeriod(LocalDate debut, LocalDate retired) {
		if (debut == null) {
			return "-";
		}
		return retired != null
				? debut.getYear() + " ~ " + retired.getYear()
				: debut.getYear() + " ~ 현재";
	}
}
