package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.domain.dto.BookmarkPageDTO;
import com.torikumilab.sumoarchive.domain.dto.RikishiBookmarkCardDTO;
import com.torikumilab.sumoarchive.domain.entity.BanzukeEntity;
import com.torikumilab.sumoarchive.domain.entity.BashoEntity;
import com.torikumilab.sumoarchive.domain.entity.RikishiEntity;
import com.torikumilab.sumoarchive.domain.entity.TorikumiEntity;
import com.torikumilab.sumoarchive.domain.entity.constant.ResultType;
import com.torikumilab.sumoarchive.repository.BanzukeRepository;
import com.torikumilab.sumoarchive.repository.BashoRepository;
import com.torikumilab.sumoarchive.repository.RikishiRepository;
import com.torikumilab.sumoarchive.repository.TorikumiRepository;
import com.torikumilab.sumoarchive.util.KimariteDisplayUtil;
import com.torikumilab.sumoarchive.util.RankDisplayUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 북마크 페이지(/bookmark)용. LocalStorage에 저장된 리키시 ID 배열을 받아 카드 목록을 만든다.
 * 정렬(드래그 앤 드롭 순서)은 클라이언트가 들고 있는 ID 순서를 그대로 존중해야 하므로,
 * 여기서는 요청받은 ids 순서대로 결과를 맞춰 돌려준다.
 *
 * <p>"오늘의 내 선수": 반즈케가 있는 가장 최근 바쇼(메인 반즈케 기본값과 같음)를 기준 바쇼로 잡고,
 * 그 바쇼의 계급·성적·경기 결과(○●□■)·마지막 경기를 카드마다 붙인다. 쿼리는 즐겨찾기 수와 무관하게
 * 바쇼 1 + 반즈케 1 + 경기 1 + 마지막 일차 1번.</p>
 */
@Service
@RequiredArgsConstructor
public class RikishiBookmarkService {

	/** 한 번에 받는 즐겨찾기 수 상한 (주소에 id를 수백 개 붙여 보내는 요청 방지) */
	static final int MAX_IDS = 50;
	private static final ZoneId ZONE = ZoneId.of("Asia/Tokyo");

	private final RikishiRepository rikishiRepository;
	private final BashoRepository bashoRepository;
	private final BanzukeRepository banzukeRepository;
	private final TorikumiRepository torikumiRepository;

	@Transactional(readOnly = true)
	public BookmarkPageDTO getBookmarkPage(List<Integer> ids) {
		return getBookmarkPage(ids, LocalDate.now(ZONE));
	}

	@Transactional(readOnly = true)
	BookmarkPageDTO getBookmarkPage(List<Integer> requested, LocalDate today) {
		List<Integer> ids = requested.stream().filter(Objects::nonNull).distinct().limit(MAX_IDS).toList();
		Map<Integer, RikishiEntity> byId = rikishiRepository.findAllById(ids).stream()
				.collect(Collectors.toMap(RikishiEntity::getId, r -> r));

		BashoEntity basho = bashoRepository.findLatestWithBanzuke().orElse(null);
		Map<Integer, BanzukeEntity> banzukeById = Map.of();
		Map<Integer, List<TorikumiEntity>> boutsById = Map.of();
		Integer lastDay = null;
		if (basho != null && !byId.isEmpty()) {
			List<Integer> found = List.copyOf(byId.keySet());
			banzukeById = banzukeRepository.findByBashoEntityIdAndRikishiEntityIdIn(basho.getId(), found).stream()
					.collect(Collectors.toMap(b -> b.getRikishiEntity().getId(), b -> b, (a, b) -> a));
			boutsById = groupByRikishi(torikumiRepository.findRegularByBashoAndRikishiIn(basho.getId(), found), byId.keySet());
			lastDay = torikumiRepository.findMaxRegularDay(basho.getId());
		}

		// 이미 삭제된 리키시 등 더 이상 존재하지 않는 id는 조용히 스킵한다.
		List<RikishiBookmarkCardDTO> cards = new ArrayList<>();
		for (Integer id : ids) {
			RikishiEntity r = byId.get(id);
			if (r != null) {
				BanzukeEntity bz = banzukeById.get(id);
				cards.add(toDto(r, bz == null ? null : current(id, bz, boutsById.getOrDefault(id, List.of()))));
			}
		}
		return new BookmarkPageDTO(basho == null ? null : bashoInfo(basho, today, lastDay), cards);
	}

	private static Map<Integer, List<TorikumiEntity>> groupByRikishi(List<TorikumiEntity> bouts, java.util.Set<Integer> ids) {
		Map<Integer, List<TorikumiEntity>> map = new java.util.HashMap<>();
		for (TorikumiEntity t : bouts) { // 일차 오름차순으로 넘어옴
			for (RikishiEntity side : List.of(t.getEastRikishiEntity(), t.getWestRikishiEntity())) {
				if (ids.contains(side.getId())) {
					map.computeIfAbsent(side.getId(), k -> new ArrayList<>()).add(t);
				}
			}
		}
		return map;
	}

	static BookmarkPageDTO.Basho bashoInfo(BashoEntity basho, LocalDate today, Integer lastDay) {
		BookmarkPageDTO.Status status;
		if (basho.getStartDate() != null && today.isBefore(basho.getStartDate())) {
			status = BookmarkPageDTO.Status.UPCOMING;
		} else if (basho.getEndDate() != null && today.isAfter(basho.getEndDate())) {
			status = BookmarkPageDTO.Status.FINISHED;
		} else {
			status = BookmarkPageDTO.Status.IN_PROGRESS;
		}
		return new BookmarkPageDTO.Basho(
				basho.getBashoYear() + "년 " + basho.getBashoMonth().getMonthValue() + "월 " + basho.getBashoMonth().getDisplayNameKr(),
				basho.getBashoYear() + "年" + basho.getBashoMonth().getDisplayNameJp(),
				basho.getBashoMonth().getDisplayNameKr(),
				basho.getBashoMonth().getDisplayNameJp(),
				status, basho.getStartDate(), lastDay);
	}

	/**
	 * 이번 바쇼 성적. 공식 성적(반즈케)은 임포트 시점 값이라 대회 중엔 경기 기록보다 뒤처질 수 있어서,
	 * 경기 기록의 승·패 수보다 적으면 경기 기록으로 센다 (RikishiDetailService와 같은 규칙).
	 */
	static RikishiBookmarkCardDTO.Current current(Integer rikishiId, BanzukeEntity bz, List<TorikumiEntity> bouts) {
		List<String> marks = new ArrayList<>();
		int wins = 0;
		int losses = 0;
		for (TorikumiEntity t : bouts) {
			if (t.getWinnerRikishiEntity() == null) {
				continue;
			}
			boolean win = rikishiId.equals(t.getWinnerRikishiEntity().getId());
			boolean fusen = t.getResultType() == ResultType.FUZEN;
			marks.add((fusen ? "FUSEN_" : "") + (win ? "WIN" : "LOSS"));
			if (win) wins++; else losses++;
		}
		int absences = 0;
		if (bz.hasRecord() && bz.getWins() + bz.getLosses() >= wins + losses) {
			wins = bz.getWins();
			losses = bz.getLosses();
			absences = bz.getAbsences();
		}
		return new RikishiBookmarkCardDTO.Current(
				withSide(RankDisplayUtil.sideDisplayKorean(bz.getSide()), RankDisplayUtil.rankDisplayKorean(bz.getRankName(), bz.getRankValue())),
				withSide(RankDisplayUtil.sideDisplay(bz.getSide()), RankDisplayUtil.rankDisplay(bz.getRankName(), bz.getRankValue())),
				wins, losses, absences, marks, lastBout(rikishiId, bouts));
	}

	private static RikishiBookmarkCardDTO.LastBout lastBout(Integer rikishiId, List<TorikumiEntity> bouts) {
		for (int i = bouts.size() - 1; i >= 0; i--) {
			TorikumiEntity t = bouts.get(i);
			if (t.getWinnerRikishiEntity() == null) {
				continue;
			}
			RikishiEntity opp = rikishiId.equals(t.getEastRikishiEntity().getId()) ? t.getWestRikishiEntity() : t.getEastRikishiEntity();
			boolean fusen = t.getResultType() == ResultType.FUZEN;
			return new RikishiBookmarkCardDTO.LastBout(
					t.getDay(),
					rikishiId.equals(t.getWinnerRikishiEntity().getId()),
					fusen,
					firstToken(opp.getShikonaKr() != null ? opp.getShikonaKr() : opp.getShikonaJp()),
					firstToken(opp.getShikonaJp() != null ? opp.getShikonaJp() : opp.getShikonaKr()),
					fusen || t.getKimarite() == null ? null : KimariteDisplayUtil.toKr(t.getKimarite()),
					fusen || t.getKimarite() == null ? null : KimariteDisplayUtil.toJp(t.getKimarite()),
					t.getId());
		}
		return null;
	}

	/** "아사노야마 히로키" / "朝乃山　広暉"처럼 이름이 붙어 있으면 시코나만 */
	private static String firstToken(String name) {
		return name == null ? null : name.strip().split("[\\s　]+")[0];
	}

	private static String withSide(String side, String rank) {
		if (rank == null) return side;
		return side == null ? rank : side + " " + rank;
	}

	private static RikishiBookmarkCardDTO toDto(RikishiEntity r, RikishiBookmarkCardDTO.Current current) {
		return new RikishiBookmarkCardDTO(
				r.getId(),
				r.getShikonaKr(),
				r.getShikonaJp(),
				r.getHeyaEntity() != null ? r.getHeyaEntity().getNameKr() : null,
				RankDisplayUtil.toKorean(r.getHighestRank()),
				formatPeriod(r.getDebutDate(), r.getRetiredDate()),
				statusLabel(r),
				r.getHeyaEntity() != null ? r.getHeyaEntity().getNameJp() : null,
				RankDisplayUtil.toJapanese(r.getHighestRank()),
				formatPeriodJp(r.getDebutDate(), r.getRetiredDate()),
				statusLabelJp(r),
				current
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

	private static String statusLabelJp(RikishiEntity r) {
		if (r.isActive()) {
			return "現役";
		}
		if (r.getOyakataNameKr() != null && !r.getOyakataNameKr().isBlank()) {
			return "親方";
		}
		return "引退";
	}

	private static String formatPeriodJp(LocalDate debut, LocalDate retired) {
		if (debut == null) {
			return "-";
		}
		return retired != null
				? debut.getYear() + " ~ " + retired.getYear()
				: debut.getYear() + " ~ 現在";
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
