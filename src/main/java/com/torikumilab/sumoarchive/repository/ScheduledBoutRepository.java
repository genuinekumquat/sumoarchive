package com.torikumilab.sumoarchive.repository;

import com.torikumilab.sumoarchive.domain.entity.ScheduledBoutEntity;
import com.torikumilab.sumoarchive.domain.entity.constant.Division;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ScheduledBoutRepository extends JpaRepository<ScheduledBoutEntity, Integer> {

	Optional<ScheduledBoutEntity> findByExternalId(String externalId);

	List<ScheduledBoutEntity> findByBashoEntityIdAndDivision(Integer bashoId, Division division);

	// "다음 경기" - 이 바쇼에서 여러 리키시의 예정 대진, 양쪽 선수 fetch, 일차 오름차순
	@Query("""
    SELECT s FROM ScheduledBoutEntity s
    JOIN FETCH s.eastRikishiEntity
    JOIN FETCH s.westRikishiEntity
    WHERE s.bashoEntity.id = :bashoId
      AND (s.eastRikishiEntity.id IN :ids OR s.westRikishiEntity.id IN :ids)
    ORDER BY s.day ASC
""")
	List<ScheduledBoutEntity> findByBashoAndRikishiIn(@Param("bashoId") Integer bashoId,
													  @Param("ids") Collection<Integer> rikishiIds);

	// 메인 "대진" 탭 - 특정 바쇼·디비전의 예정 대진, 양쪽 선수 fetch
	@Query("""
    SELECT s FROM ScheduledBoutEntity s
    JOIN FETCH s.eastRikishiEntity
    JOIN FETCH s.westRikishiEntity
    WHERE s.bashoEntity.id = :bashoId
      AND s.division = :division
    ORDER BY s.day ASC, s.matchNo ASC
""")
	List<ScheduledBoutEntity> findByBashoAndDivisionFetch(@Param("bashoId") Integer bashoId,
														  @Param("division") Division division);

	// 이 바쇼에 발표된 예정 대진 중 가장 이른 일차 (없으면 null)
	@Query("SELECT MIN(s.day) FROM ScheduledBoutEntity s WHERE s.bashoEntity.id = :bashoId")
	Integer findMinDay(@Param("bashoId") Integer bashoId);
}
