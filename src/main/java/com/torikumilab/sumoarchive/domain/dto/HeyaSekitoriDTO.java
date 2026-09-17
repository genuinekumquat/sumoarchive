package com.torikumilab.sumoarchive.domain.dto;

import java.util.List;

/**
 * 일문 안의 헤야 한 곳. sekitori가 비어있어도(현재 세키토리가 없는 헤야) 목록에 포함된다 -
 * "일문별 헤야 전체 구조"를 보여주는 게 목적이라 세키토리 유무로 헤야 자체를 거르지 않는다.
 */
public record HeyaSekitoriDTO(
		Integer heyaId,
		String heyaNameKr,
		String heyaNameJp,
		List<SekitoriDTO> sekitori // 마쿠우치 먼저, 그다음 주료 (각각 순위순)
) {
}
