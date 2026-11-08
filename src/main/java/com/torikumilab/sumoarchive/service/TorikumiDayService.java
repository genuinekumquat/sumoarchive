package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.domain.dto.TorikumiDayDTO;
import com.torikumilab.sumoarchive.domain.entity.BanzukeEntity;
import com.torikumilab.sumoarchive.domain.entity.BashoEntity;
import com.torikumilab.sumoarchive.domain.entity.RikishiEntity;
import com.torikumilab.sumoarchive.domain.entity.ScheduledBoutEntity;
import com.torikumilab.sumoarchive.domain.entity.TorikumiEntity;
import com.torikumilab.sumoarchive.domain.entity.constant.Division;
import com.torikumilab.sumoarchive.domain.entity.constant.ResultType;
import com.torikumilab.sumoarchive.repository.BanzukeRepository;
import com.torikumilab.sumoarchive.repository.BashoRepository;
import com.torikumilab.sumoarchive.repository.RikishiShikonaHistoryRepository;
import com.torikumilab.sumoarchive.repository.ScheduledBoutRepository;
import com.torikumilab.sumoarchive.repository.TorikumiRepository;
import com.torikumilab.sumoarchive.util.KimariteDisplayUtil;
import com.torikumilab.sumoarchive.util.RankDisplayUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * 메인 "대진" 탭. 한 바쇼·디비전의 하루치 경기를 결과(torikumi)와 예정 대진(scheduled_bout)에서 모아
 * 실제 경기 순서(match_no)대로 돌려준다. 선수 옆 성적은 그 경기 전날까지의 정규 경기 기준(반즈케표·중계 방식).
 *
 * <p>쿼리: 바쇼 1 + 그 디비전 경기 전부 1(최대 수백 건) + 예정 대진 1 + 그날 선수 반즈케 1.
 * 결과가 매일 바뀌어서 캐시는 쓰지 않는다.</p>
 */
@Service
@RequiredArgsConstructor
public class TorikumiDayService {

	/** 우승 결정전은 16일째로 들어온다 (TorikumiImportService) */
	static final int PLAYOFF_DAY = 16;

	private final BashoRepository bashoRepository;
	private final TorikumiRepository torikumiRepository;
	private final ScheduledBoutRepository scheduledBoutRepository;
	private final BanzukeRepository banzukeRepository;
	private final RikishiShikonaHistoryRepository rikishiShikonaHistoryRepository;

	/**
	 * @param bashoId 없으면 반즈케가 있는 가장 최근 바쇼
	 * @param day     없으면 기본 일차(결과가 있으면 마지막으로 치른 날, 없으면 첫 예정일)
	 */
	@Transactional(readOnly = true)
	public TorikumiDayDTO getDay(Integer bashoId, Division division, Integer day) {
		BashoEntity basho = bashoId != null
				? bashoRepository.findById(bashoId).orElseThrow(() -> new EntityNotFoundException("바쇼를 찾을 수 없습니다. id=" + bashoId))
				: bashoRepository.findLatestWithBanzuke().orElse(null);
		if (basho == null) {
			return new TorikumiDayDTO(null, null, null, null, List.of(), List.of());
		}

		List<TorikumiEntity> done = torikumiRepository.findByBashoAndDivisionFetch(basho.getId(), division);
		List<ScheduledBoutEntity> scheduled = scheduledBoutRepository.findByBashoAndDivisionFetch(basho.getId(), division);
		List<TorikumiDayDTO.DayStatus> days = dayStatuses(done, scheduled);
		Integer shown = day != null && days.stream().anyMatch(d -> d.day() == day) ? day : defaultDay(days);

		String labelKr = basho.getBashoYear() + "년 " + basho.getBashoMonth().getMonthValue() + "월 " + basho.getBashoMonth().getDisplayNameKr();
		String labelJp = basho.getBashoYear() + "年" + basho.getBashoMonth().getDisplayNameJp();
		if (shown == null) {
			return new TorikumiDayDTO(basho.getId(), labelKr, labelJp, null, days, List.of());
		}

		// 그날 경기: 결과 + (아직 결과 없는) 예정, 경기 순서대로
		List<Row> rows = new ArrayList<>();
		Set<String> doneKeys = new HashSet<>();
		for (TorikumiEntity t : done) {
			if (t.getDay().equals(shown)) {
				rows.add(new Row(t.getMatchNo(), t.getEastRikishiEntity(), t.getWestRikishiEntity(), t, null));
				doneKeys.add(pairKey(t.getEastRikishiEntity(), t.getWestRikishiEntity()));
			}
		}
		for (ScheduledBoutEntity s : scheduled) {
			if (s.getDay().equals(shown) && !doneKeys.contains(pairKey(s.getEastRikishiEntity(), s.getWestRikishiEntity()))) {
				rows.add(new Row(s.getMatchNo(), s.getEastRikishiEntity(), s.getWestRikishiEntity(), null, s));
			}
		}
		rows.sort(Comparator.comparing(Row::matchNo, Comparator.nullsLast(Comparator.naturalOrder())));

		// 이름 옆 성적: 결과가 나온 경기는 그날까지(이긴 쪽이 1-0), 아직 안 치른 예정 경기는 그 경기 전까지
		Map<Integer, int[]> recordsBefore = recordsBefore(done, shown);
		Map<Integer, int[]> recordsThrough = recordsBefore(done, shown + 1);
		Set<Integer> ids = new HashSet<>();
		rows.forEach(r -> { ids.add(r.east().getId()); ids.add(r.west().getId()); });
		Map<Integer, BanzukeEntity> banzuke = ids.isEmpty() ? Map.of()
				: NextBouts.byRikishiId(banzukeRepository.findByBashoEntityIdAndRikishiEntityIdIn(basho.getId(), List.copyOf(ids)));

		// 지난 바쇼를 고르면 이름도 그 바쇼 당시 시코나로 (예: 2025년 하쿠노후지 → 하쿠오호)
		ShikonaAtBasho oldNames = ShikonaAtBasho.load(rikishiShikonaHistoryRepository, ids);
		List<TorikumiDayDTO.Bout> bouts = rows.stream()
				.map(r -> toBout(r, r.done() != null ? recordsThrough : recordsBefore, banzuke, oldNames, basho.getStartDate()))
				.toList();
		return new TorikumiDayDTO(basho.getId(), labelKr, labelJp, shown, days, bouts);
	}

	private record Row(Integer matchNo, RikishiEntity east, RikishiEntity west, TorikumiEntity done, ScheduledBoutEntity scheduled) {}

	private static String pairKey(RikishiEntity a, RikishiEntity b) {
		return a.getId() + "-" + b.getId();
	}

	/** 결과가 있는 날은 DONE, 예정 대진만 있는 날은 SCHEDULED */
	static List<TorikumiDayDTO.DayStatus> dayStatuses(List<TorikumiEntity> done, List<ScheduledBoutEntity> scheduled) {
		Map<Integer, TorikumiDayDTO.Status> byDay = new TreeMap<>();
		scheduled.forEach(s -> byDay.put(s.getDay(), TorikumiDayDTO.Status.SCHEDULED));
		done.forEach(t -> byDay.put(t.getDay(), TorikumiDayDTO.Status.DONE));
		return byDay.entrySet().stream().map(e -> new TorikumiDayDTO.DayStatus(e.getKey(), e.getValue())).toList();
	}

	/**
	 * 결과가 있으면 마지막으로 치른 날, 아직 없으면(개막 전) 발표된 예정일 중 가장 이른 날.
	 * 바쇼 중에는 결과를 보러 오는 사람이 많아서 결과를 먼저 보여 주고, 다음 날 대진은 › 버튼으로 넘겨 본다.
	 * 결정전은 기본값으로 고르지 않는다.
	 */
	static Integer defaultDay(List<TorikumiDayDTO.DayStatus> days) {
		Integer firstScheduled = null;
		Integer lastDone = null;
		for (TorikumiDayDTO.DayStatus d : days) {
			if (d.status() == TorikumiDayDTO.Status.SCHEDULED && firstScheduled == null) {
				firstScheduled = d.day();
			}
			if (d.status() == TorikumiDayDTO.Status.DONE && d.day() < PLAYOFF_DAY) {
				lastDone = d.day();
			}
		}
		if (lastDone != null) {
			return lastDone;
		}
		if (firstScheduled != null) {
			return firstScheduled;
		}
		return days.isEmpty() ? null : days.get(days.size() - 1).day();
	}

	/** rikishiId → {승, 패}: 이 일차 전날까지의 정규 경기(결정전 제외, 부전 포함). 그날까지 포함하려면 day + 1 */
	static Map<Integer, int[]> recordsBefore(List<TorikumiEntity> done, int day) {
		Map<Integer, int[]> map = new HashMap<>();
		for (TorikumiEntity t : done) {
			if (t.getDay() >= day || t.isExtraMatch() || t.getWinnerRikishiEntity() == null) {
				continue;
			}
			Integer winner = t.getWinnerRikishiEntity().getId();
			for (RikishiEntity side : List.of(t.getEastRikishiEntity(), t.getWestRikishiEntity())) {
				int[] wl = map.computeIfAbsent(side.getId(), k -> new int[2]);
				wl[side.getId().equals(winner) ? 0 : 1]++;
			}
		}
		return map;
	}

	private static TorikumiDayDTO.Bout toBout(Row r, Map<Integer, int[]> records, Map<Integer, BanzukeEntity> banzuke,
											  ShikonaAtBasho oldNames, LocalDate bashoStart) {
		TorikumiDayDTO.Side east = side(r.east(), records, banzuke, oldNames.at(r.east().getId(), bashoStart));
		TorikumiDayDTO.Side west = side(r.west(), records, banzuke, oldNames.at(r.west().getId(), bashoStart));
		if (r.done() == null) {
			return new TorikumiDayDTO.Bout(east, west, true, null, false, null, null, null);
		}
		TorikumiEntity t = r.done();
		String winner = t.getWinnerRikishiEntity() == null ? null
				: t.getWinnerRikishiEntity().getId().equals(r.east().getId()) ? "EAST" : "WEST";
		boolean fusen = t.getResultType() == ResultType.FUZEN;
		String k = fusen || t.getKimarite() == null ? null : t.getKimarite();
		return new TorikumiDayDTO.Bout(east, west, false, winner, fusen,
				k == null ? null : KimariteDisplayUtil.toKr(k), k == null ? null : KimariteDisplayUtil.toJp(k), t.getId());
	}

	/** @param oldName 그 바쇼 당시 옛 시코나 (지금 이름이면 null) */
	private static TorikumiDayDTO.Side side(RikishiEntity r, Map<Integer, int[]> records, Map<Integer, BanzukeEntity> banzuke,
											ShikonaAtBasho.Name oldName) {
		int[] wl = records.getOrDefault(r.getId(), new int[2]);
		BanzukeEntity b = banzuke.get(r.getId());
		return new TorikumiDayDTO.Side(
				r.getId(),
				oldName != null ? oldName.kr()
						: NextBouts.firstToken(r.getShikonaKr() != null ? r.getShikonaKr() : r.getShikonaJp() != null ? r.getShikonaJp() : r.getShikonaEn()),
				oldName != null ? oldName.jp()
						: NextBouts.firstToken(r.getShikonaJp() != null ? r.getShikonaJp() : r.getShikonaEn()),
				b == null ? null : NextBouts.withSide(RankDisplayUtil.sideDisplayKorean(b.getSide()), RankDisplayUtil.rankDisplayKorean(b.getRankName(), b.getRankValue())),
				b == null ? null : NextBouts.withSide(RankDisplayUtil.sideDisplay(b.getSide()), RankDisplayUtil.rankDisplay(b.getRankName(), b.getRankValue())),
				wl[0], wl[1]);
	}
}
