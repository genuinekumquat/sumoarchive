package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.domain.dto.BashoGameLogDTO;
import com.torikumilab.sumoarchive.domain.dto.KimariteCountRow;
import com.torikumilab.sumoarchive.domain.dto.KimariteStatDTO;
import com.torikumilab.sumoarchive.domain.dto.MatchHistoryItemDTO;
import com.torikumilab.sumoarchive.domain.dto.RikishiDetailDTO;
import com.torikumilab.sumoarchive.domain.dto.ShikonaHistoryItemDTO;
import com.torikumilab.sumoarchive.domain.entity.BanzukeEntity;
import com.torikumilab.sumoarchive.domain.entity.BashoEntity;
import com.torikumilab.sumoarchive.domain.entity.RikishiEntity;
import com.torikumilab.sumoarchive.domain.entity.TorikumiEntity;
import com.torikumilab.sumoarchive.domain.entity.constant.AwardType;
import com.torikumilab.sumoarchive.domain.entity.constant.Division;
import com.torikumilab.sumoarchive.domain.entity.constant.ResultType;
import com.torikumilab.sumoarchive.domain.entity.constant.Side;
import com.torikumilab.sumoarchive.repository.AwardRepository;
import com.torikumilab.sumoarchive.repository.BanzukeRepository;
import com.torikumilab.sumoarchive.repository.KinboshiRepository;
import com.torikumilab.sumoarchive.repository.RikishiRepository;
import com.torikumilab.sumoarchive.repository.RikishiShikonaHistoryRepository;
import com.torikumilab.sumoarchive.repository.TorikumiRepository;
import com.torikumilab.sumoarchive.util.RankDisplayUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 리키시 상세 프로필(/rikishi/{id}) 조립 서비스.
 * ⚠ RikishiEntity/BanzukeEntity 등의 실제 필드·getter명이 다르면 컴파일 에러가 날 수 있습니다 —
 *   DataSeeder에서 쓰인 이름(shikonaKr, birthdate, heyaEntity, isActive, debutDate 등) 기준으로 작성했어요.
 */
@Service
@RequiredArgsConstructor
public class RikishiDetailService {
	
	// 원형차트 배색(PALETTE, detail.html)이 10개까지 고유 색상을 갖고 있어서 그 개수에 맞춤.
	// 이보다 더 다양한 결정기술이 나오는 리키시(커리어가 아주 긴 경우)만 "기타"로 묶임.
	private static final int TOP_KIMARITE_COUNT = 10;
	
	private final RikishiRepository rikishiRepository;
	private final RikishiShikonaHistoryRepository rikishiShikonaHistoryRepository;
	private final BanzukeRepository banzukeRepository;
	private final TorikumiRepository torikumiRepository;
	private final AwardRepository awardRepository;
	private final KinboshiRepository kinboshiRepository;
	
	public RikishiDetailDTO getRikishiDetail(Integer rikishiId) {
		RikishiEntity r = rikishiRepository.findById(rikishiId)
				.orElseThrow(() -> new EntityNotFoundException("리키시를 찾을 수 없습니다. id=" + rikishiId));
		
		List<ShikonaHistoryItemDTO> history = rikishiShikonaHistoryRepository
				.findByRikishiEntityIdOrderByValidFromAsc(rikishiId)
				.stream()
				.map(h -> new ShikonaHistoryItemDTO(h.getShikonaKr(), h.getValidFrom(), h.getValidTo()))
				.toList();
		
		// 최신 바쇼 기준 반즈케 (은퇴/오야카타면 없음 = null)
		List<BanzukeEntity> banzukeHistory = banzukeRepository.findByRikishiIdOrderByBashoDesc(rikishiId);
		BanzukeEntity latest = banzukeHistory.isEmpty() ? null : banzukeHistory.get(0);
		
		long wins = torikumiRepository.countByWinnerRikishiEntityIdAndIsExtraMatchFalse(rikishiId);
		long losses = torikumiRepository.countByLoserRikishiEntityIdAndIsExtraMatchFalse(rikishiId);
		long bashoCount = banzukeRepository.countByRikishiId(rikishiId);
		
		long yushoMakuuchi = awardRepository.countByRikishiEntityIdAndDivisionAndAwardType(
				rikishiId, Division.Makuuchi, AwardType.YUSHO);
		long yushoJuryo = awardRepository.countByRikishiEntityIdAndDivisionAndAwardType(
				rikishiId, Division.Juryo, AwardType.YUSHO);
		long sanshoShukun = awardRepository.countByRikishiEntityIdAndAwardType(rikishiId, AwardType.SANSHO_SHUKUN);
		long sanshoKanto = awardRepository.countByRikishiEntityIdAndAwardType(rikishiId, AwardType.SANSHO_KANTO);
		long sanshoGino = awardRepository.countByRikishiEntityIdAndAwardType(rikishiId, AwardType.SANSHO_GINO);
		long kinboshiCount = kinboshiRepository.countByWinnerRikishiEntityId(rikishiId);
		
		Integer age = r.getBirthdate() != null
				? Period.between(r.getBirthdate(), LocalDate.now()).getYears()
				: null;
		
		return RikishiDetailDTO.builder()
				.id(r.getId())
				.shikonaKr(r.getShikonaKr())
				.shikonaJp(r.getShikonaJp())
				.name(r.getName())
				.birthdate(r.getBirthdate())
				.age(age)
				.birthplace(r.getBirthplace())
				.nationality(r.getNationality())
				.heyaNameKr(r.getHeyaEntity() != null ? r.getHeyaEntity().getNameKr() : null)
				.height(r.getHeight())
				.weight(r.getWeight())
				.fightingStyle(r.getFightingStyle())
				.debutDate(r.getDebutDate())
				.highestRank(r.getHighestRank())
				.isActive(r.isActive())
				.retiredDate(r.getRetiredDate())
				.oyakataNameKr(r.getOyakataNameKr())
				.photoUrl(r.getPhotoUrl())
				.sideDisplay(latest != null ? RankDisplayUtil.sideDisplay(latest.getSide()) : null)
				.rankDisplay(latest != null ? RankDisplayUtil.rankDisplay(latest.getRankName(), latest.getRankValue()) : null)
				.shikonaHistory(history)
				.totalWins(wins)
				.totalLosses(losses)
				.totalAbsences(0L) // ⚠ 현재 스키마에 휴장(休場) 기록이 없어 항상 0으로 내려감
				.totalBasho(bashoCount)
				.yushoMakuuchi(yushoMakuuchi)
				.yushoJuryo(yushoJuryo)
				.sanshoShukun(sanshoShukun)
				.sanshoKanto(sanshoKanto)
				.sanshoGino(sanshoGino)
				.kinboshiCount(kinboshiCount)
				.build();
	}
	
	/**
	 * 결정기술(키마리테) 원형차트용 통계. 등록된 대전 기록이 없으면 빈 리스트.
	 * 서로 다른 결정기술이 {@value #TOP_KIMARITE_COUNT}개 이하면 전부 개별로 보여주고,
	 * 그보다 많을 때만 상위 {@value #TOP_KIMARITE_COUNT}개는 개별로, 나머지는 "기타"로 합쳐서 반환.
	 */
	public List<KimariteStatDTO> getKimariteStats(Integer rikishiId) {
		List<KimariteCountRow> rows = torikumiRepository.findKimariteStats(rikishiId);
		
		long total = rows.stream().mapToLong(KimariteCountRow::getCnt).sum();
		if (total == 0) {
			return List.of();
		}
		
		List<KimariteCountRow> sorted = rows.stream()
				.sorted(Comparator.comparingLong(KimariteCountRow::getCnt).reversed())
				.toList();
		
		List<KimariteCountRow> top = sorted.stream().limit(TOP_KIMARITE_COUNT).toList();
		long topSum = top.stream().mapToLong(KimariteCountRow::getCnt).sum();
		long etcCount = total - topSum;
		
		List<KimariteStatDTO> result = new ArrayList<>();
		for (KimariteCountRow row : top) {
			result.add(new KimariteStatDTO(row.getKimarite(), row.getCnt(), round1(row.getCnt() * 100.0 / total)));
		}
		if (etcCount > 0) {
			result.add(new KimariteStatDTO("기타", etcCount, round1(etcCount * 100.0 / total)));
		}
		return result;
	}
	
	private double round1(double value) {
		return Math.round(value * 10) / 10.0;
	}
	
	/**
	 * 리키시 상세 화면 하단 "바쇼별 성적(星取表)". 이 리키시가 반즈케에 오른 바쇼마다 한 줄씩,
	 * 최신 바쇼부터 내림차순으로 반환 (여러 바쇼를 한 번에 훑어볼 수 있도록). 반즈케 자체가 없으면 빈 리스트.
	 */
	public List<BashoGameLogDTO> getGameLog(Integer rikishiId) {
		List<BanzukeEntity> banzukeHistory = banzukeRepository.findByRikishiIdOrderByBashoDesc(rikishiId);
		if (banzukeHistory.isEmpty()) {
			return List.of();
		}
		return banzukeHistory.stream()
				.map(b -> toBashoGameLog(rikishiId, b))
				.toList();
	}
	
	private BashoGameLogDTO toBashoGameLog(Integer rikishiId, BanzukeEntity banzuke) {
		List<MatchHistoryItemDTO> matches = buildMatchHistory(rikishiId, banzuke.getBashoEntity().getId());
		
		long wins = matches.stream().filter(m -> m.status() == MatchHistoryItemDTO.Status.WIN).count();
		long losses = matches.stream().filter(m -> m.status() == MatchHistoryItemDTO.Status.LOSS).count();
		long absences = matches.stream().filter(m -> m.status() == MatchHistoryItemDTO.Status.ABSENT).count();
		String recordSummary = matches.isEmpty()
				? "-"
				: wins + "勝" + losses + "敗" + (absences > 0 ? absences + "休" : "");
		
		BashoEntity basho = banzuke.getBashoEntity();
		String bashoLabel = basho.getBashoYear() + "年"
				+ String.format("%02d", basho.getBashoMonth().getMonthValue()) + "月場所";
		
		return new BashoGameLogDTO(
				bashoLabel,
				RankDisplayUtil.rankDisplay(banzuke.getRankName(), banzuke.getRankValue()),
				recordSummary,
				matches
		);
	}
	
	/**
	 * 특정 바쇼 하나의 대전 목록을 day 1~15 오름차순으로 조립.
	 * 이 리키시가 그 바쇼에 등록된 토리쿠미가 하나도 없으면 빈 리스트(= "기록 없음" 처리).
	 * 등록된 게 있는 바쇼인데 특정 날짜만 비어 있으면(= 첫 출전일 이후 공백) 휴장(ABSENT)으로 채워서 반환.
	 * ⚠ 스키마에 별도 휴장 테이블/컬럼이 없어서 "그 바쇼의 첫 출전일 이후 토리쿠미 공백 = 휴장"으로
	 *   추론하는 방식입니다. 첫 출전일 이전 공백(예: 데이터가 아직 다 안 채워진 경우)은 휴장으로
	 *   보지 않고 그냥 표에서 생략합니다.
	 */
	private List<MatchHistoryItemDTO> buildMatchHistory(Integer rikishiId, Integer bashoId) {
		List<TorikumiEntity> matches = torikumiRepository.findMatchHistory(rikishiId, bashoId);
		if (matches.isEmpty()) {
			return List.of();
		}
		
		List<Integer> opponentIds = matches.stream()
				.map(t -> resolveOpponent(t, rikishiId).getId())
				.distinct()
				.toList();
		
		Map<Integer, BanzukeEntity> opponentBanzukeByRikishiId = banzukeRepository
				.findByBashoEntityIdAndRikishiEntityIdIn(bashoId, opponentIds)
				.stream()
				.collect(Collectors.toMap(b -> b.getRikishiEntity().getId(), b -> b));
		
		Map<Integer, TorikumiEntity> matchByDay = matches.stream()
				.collect(Collectors.toMap(TorikumiEntity::getDay, t -> t));
		int firstDay = matches.get(0).getDay(); // findMatchHistory가 day ASC로 내려주므로 첫 번째가 첫 출전일
		
		List<MatchHistoryItemDTO> result = new ArrayList<>();
		for (int day = 1; day <= 15; day++) {
			TorikumiEntity t = matchByDay.get(day);
			if (t != null) {
				result.add(toMatchHistoryItem(t, rikishiId, opponentBanzukeByRikishiId));
			} else if (day >= firstDay) {
				result.add(new MatchHistoryItemDTO(day, null, null, null, null, null,
						MatchHistoryItemDTO.Status.ABSENT, "休場"));
			}
		}
		return result;
	}
	
	private MatchHistoryItemDTO toMatchHistoryItem(TorikumiEntity t, Integer rikishiId,
												   Map<Integer, BanzukeEntity> opponentBanzukeByRikishiId) {
		RikishiEntity opponent = resolveOpponent(t, rikishiId);
		BanzukeEntity opponentBanzuke = opponentBanzukeByRikishiId.get(opponent.getId());
		
		boolean win = t.getWinnerRikishiEntity() != null
				&& t.getWinnerRikishiEntity().getId().equals(rikishiId);
		
		Side mySide = t.getEastRikishiEntity().getId().equals(rikishiId) ? Side.EAST : Side.WEST;
		
		String resultDisplay;
		if (t.getResultType() == ResultType.FUZEN) {
			resultDisplay = win ? "不戦勝" : "不戦敗";
		} else {
			resultDisplay = t.getKimarite() != null ? t.getKimarite() : "-";
		}
		
		return new MatchHistoryItemDTO(
				t.getDay(),
				t.getId(),
				opponent.getId(),
				firstToken(opponent.getShikonaKr()),
				opponentBanzuke != null
						? RankDisplayUtil.rankDisplay(opponentBanzuke.getRankName(), opponentBanzuke.getRankValue())
						: null,
				mySide,
				win ? MatchHistoryItemDTO.Status.WIN : MatchHistoryItemDTO.Status.LOSS,
				resultDisplay
		);
	}
	
	// east/west 중 rikishiId가 아닌 쪽을 상대로 반환
	private RikishiEntity resolveOpponent(TorikumiEntity t, Integer rikishiId) {
		return t.getEastRikishiEntity().getId().equals(rikishiId)
				? t.getWestRikishiEntity()
				: t.getEastRikishiEntity();
	}
	
	// shikonaKr에 공백으로 구분된 성명이 같이 들어있는 경우(예: "아사노야마 히로키") 호시토리표/슬라이드 패널처럼
	// 공간이 좁거나 시코나만 필요한 곳에서는 공백 앞부분(시코나)만 잘라서 보여줌.
	private String firstToken(String value) {
		if (value == null) {
			return null;
		}
		int idx = value.indexOf(' ');
		return idx > 0 ? value.substring(0, idx) : value;
	}
}