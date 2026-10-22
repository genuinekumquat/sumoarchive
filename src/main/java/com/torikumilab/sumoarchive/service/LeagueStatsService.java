package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.domain.entity.constant.Division;
import com.torikumilab.sumoarchive.repository.TorikumiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 리키시 페이지가 쓰는 리그 전체 집계 (패배 유형의 디비전별 결정기술 분포, 체격 차별 승률, 바쇼별 적재 일차).
 *
 * <p>어느 리키시 페이지에서나 결과가 같은데 전체 경기(10만여 건)를 매번 집계해서 프로필 응답의 대부분을
 * 차지했다(로컬 측정: 체격 차 집계만 1.1초) → 한 번 계산해 {@value #CACHE}에 담는다.
 * 경기·키·체중이 바뀌는 곳(토리쿠미 임포트·관리자 토리쿠미 수정·로스터 동기화·관리자 리키시 수정)에서 비운다.</p>
 *
 * <p>캐시에는 쿼리 프로젝션 대신 값만 담은 record를 넣는다.</p>
 */
@Service
@RequiredArgsConstructor
public class LeagueStatsService {

	public static final String CACHE = "leagueStats";

	private final TorikumiRepository torikumiRepository;

	public record DivisionKimariteCount(Division division, String kimarite, long count) {}

	/** 체중 차·키 차(서쪽 - 동쪽)별 경기 수와 동쪽 승수. 차이를 모르면 null */
	public record PhysiqueDiff(Double weightDiff, Double heightDiff, long bouts, long eastWins) {}

	@Cacheable(value = CACHE, key = "'kimariteByDivision'")
	public List<DivisionKimariteCount> kimariteByDivision() {
		return torikumiRepository.findKimariteByDivision().stream()
				.map(r -> new DivisionKimariteCount(r.getDivision(), r.getKimarite(), r.getCnt()))
				.toList();
	}

	/**
	 * 호시토리표용 - 바쇼별·지위별 적재된 마지막 일차 (결정전 제외). 행이 없으면 그 바쇼·지위 토리쿠미가 아직 없음.
	 * 리키시와 무관한 값인데 베테랑 페이지에선 거의 전 경기를 훑어서(로컬 0.15초) 여기서 한 번만 계산한다.
	 */
	@Cacheable(value = CACHE, key = "'loadedDays'")
	public Map<Integer, Map<Division, Integer>> loadedDaysByBasho() {
		Map<Integer, Map<Division, Integer>> result = new HashMap<>();
		for (TorikumiRepository.LoadedDayRow row : torikumiRepository.findAllLoadedDays()) {
			result.computeIfAbsent(row.getBashoId(), k -> new EnumMap<>(Division.class))
					.put(row.getDivision(), row.getLastDay());
		}
		return Collections.unmodifiableMap(result);
	}

	@Cacheable(value = CACHE, key = "'physiqueDiff'")
	public List<PhysiqueDiff> physiqueDiff() {
		return torikumiRepository.findPhysiqueDiffStats().stream()
				.map(r -> new PhysiqueDiff(
						r.getWeightDiff() != null ? r.getWeightDiff().doubleValue() : null,
						r.getHeightDiff() != null ? r.getHeightDiff().doubleValue() : null,
						r.getBouts().longValue(),
						r.getEastWins().longValue()))
				.toList();
	}
}
