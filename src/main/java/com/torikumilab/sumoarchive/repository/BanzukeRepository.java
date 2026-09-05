package com.torikumilab.sumoarchive.repository;

import com.torikumilab.sumoarchive.domain.entity.BanzukeEntity;
import com.torikumilab.sumoarchive.domain.entity.constant.Division;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BanzukeRepository extends JpaRepository<BanzukeEntity, Integer> {
	
	@Query("""
        SELECT b FROM BanzukeEntity b
        JOIN FETCH b.rikishiEntity r
        WHERE b.bashoEntity.id = :bashoId
        AND b.division = :division
        ORDER BY
            CASE b.rankName
                WHEN com.torikumilab.sumoarchive.domain.entity.constant.RankName.Yokozuna THEN 1
                WHEN com.torikumilab.sumoarchive.domain.entity.constant.RankName.Ozeki THEN 2
                WHEN com.torikumilab.sumoarchive.domain.entity.constant.RankName.Sekiwake THEN 3
                WHEN com.torikumilab.sumoarchive.domain.entity.constant.RankName.Komusubi THEN 4
                WHEN com.torikumilab.sumoarchive.domain.entity.constant.RankName.Maegashira THEN 5
                ELSE 6
            END,
            b.rankValue ASC,
            b.side ASC
    """)
	List<BanzukeEntity> findByBashoAndDivisionOrdered(
			@Param("bashoId") Integer bashoId,
			@Param("division") Division division
	);
	
	
	// 최신 반즈케 확인용 (바쇼 시작일 내림차순, 서비스에서 첫 번째 요소만 사용)
	@Query("""
    SELECT b FROM BanzukeEntity b
    WHERE b.rikishiEntity.id = :rikishiId
    ORDER BY b.bashoEntity.startDate DESC
""")
	List<BanzukeEntity> findByRikishiIdOrderByBashoDesc(@Param("rikishiId") Integer rikishiId);
	
	// 통산 출전 바쇼 수
	@Query("SELECT COUNT(b) FROM BanzukeEntity b WHERE b.rikishiEntity.id = :rikishiId")
	long countByRikishiId(@Param("rikishiId") Integer rikishiId);
	
	// 호시토리표(대전표) 상대들의 그 바쇼 기준 반즈케를 한 번에 조회 (N+1 방지용 배치 조회)
	List<BanzukeEntity> findByBashoEntityIdAndRikishiEntityIdIn(Integer bashoId, List<Integer> rikishiIds);
	
}