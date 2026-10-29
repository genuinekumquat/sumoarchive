package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.domain.entity.RikishiShikonaHistoryEntity;
import com.torikumilab.sumoarchive.repository.RikishiShikonaHistoryRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 지난 경기·반즈케를 「그 바쇼 당시 시코나」로 보여 주기 위한 조회표 (rikishi_shikona_history 기준).
 * 예: 하쿠노후지(伯乃富士)는 2025년 바쇼에서는 하쿠오호(伯桜鵬)로 나와야 한다.
 *
 * <p>화면에 나오는 리키시들의 이력을 한 번에 읽어 두고 메모리에서 찾는다 (경기·바쇼 수만큼 조회하지 않게).
 * 기간이 비어 있는 이력은 언제 쓰던 이름인지 알 수 없어 쓰지 않는다.</p>
 */
public final class ShikonaAtBasho {

	/** 당시 시코나 (한글·일본어) */
	public record Name(String kr, String jp) {}

	private static final ShikonaAtBasho EMPTY = new ShikonaAtBasho(Map.of());

	private final Map<Integer, List<RikishiShikonaHistoryEntity>> byRikishiId;

	private ShikonaAtBasho(Map<Integer, List<RikishiShikonaHistoryEntity>> byRikishiId) {
		this.byRikishiId = byRikishiId;
	}

	public static ShikonaAtBasho load(RikishiShikonaHistoryRepository repository, Collection<Integer> rikishiIds) {
		if (rikishiIds == null || rikishiIds.isEmpty()) {
			return EMPTY;
		}
		return new ShikonaAtBasho(repository.findByRikishiEntityIdIn(rikishiIds).stream()
				.filter(h -> h.getValidFrom() != null && h.getValidTo() != null)
				.collect(Collectors.groupingBy(h -> h.getRikishiEntity().getId())));
	}

	/** date(바쇼 시작일)에 옛 시코나를 쓰고 있었으면 그 이름, 아니면 null (= 지금 시코나 그대로) */
	public Name at(Integer rikishiId, LocalDate date) {
		if (rikishiId == null || date == null) {
			return null;
		}
		for (RikishiShikonaHistoryEntity h : byRikishiId.getOrDefault(rikishiId, List.of())) {
			if (!date.isBefore(h.getValidFrom()) && !date.isAfter(h.getValidTo())) {
				return new Name(h.getShikonaKr(), h.getShikonaJp());
			}
		}
		return null;
	}
}
