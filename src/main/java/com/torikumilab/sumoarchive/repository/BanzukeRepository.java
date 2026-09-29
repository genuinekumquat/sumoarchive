package com.torikumilab.sumoarchive.repository;

import com.torikumilab.sumoarchive.domain.entity.BanzukeEntity;
import com.torikumilab.sumoarchive.domain.entity.constant.Division;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface BanzukeRepository extends JpaRepository<BanzukeEntity, Integer> {

	/** sitemap.xml용: 해당 지위(마쿠우치·쥬료)로 반즈케에 한 번이라도 오른 리키시 id */
	@Query("SELECT DISTINCT b.rikishiEntity.id FROM BanzukeEntity b WHERE b.division IN :divisions ORDER BY b.rikishiEntity.id")
	List<Integer> findDistinctRikishiIdsByDivisionIn(@Param("divisions") Collection<Division> divisions);

	@Query("""
        SELECT b FROM BanzukeEntity b
        JOIN FETCH b.rikishiEntity r
        LEFT JOIN FETCH r.heyaEntity
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
	
	
	// 최신 반즈케 확인용 (바쇼 시작일 내림차순, 서비스에서 첫 번째 요소만 사용).
	// 호시토리표가 행마다 바쇼 정보를 읽으므로 바쇼를 같이 fetch (행마다 basho 지연 로딩 쿼리 방지).
	@Query("""
    SELECT b FROM BanzukeEntity b
    JOIN FETCH b.bashoEntity
    WHERE b.rikishiEntity.id = :rikishiId
    ORDER BY b.bashoEntity.startDate DESC
""")
	List<BanzukeEntity> findByRikishiIdOrderByBashoDesc(@Param("rikishiId") Integer rikishiId);
	
	// 통산 출전 바쇼 수
	@Query("SELECT COUNT(b) FROM BanzukeEntity b WHERE b.rikishiEntity.id = :rikishiId")
	long countByRikishiId(@Param("rikishiId") Integer rikishiId);
	
	// 호시토리표(대전표) 상대들의 그 바쇼 기준 반즈케를 한 번에 조회 (N+1 방지용 배치 조회)
	List<BanzukeEntity> findByBashoEntityIdAndRikishiEntityIdIn(Integer bashoId, List<Integer> rikishiIds);

	// 호시토리표 전 바쇼의 상대 반즈케를 한 번에 조회. (바쇼, 리키시) 조합이 실제 대전보다 넓게 잡힐 수 있어
	// 서비스에서 (bashoId, rikishiId)로 골라 쓴다.
	List<BanzukeEntity> findByBashoEntityIdInAndRikishiEntityIdIn(Collection<Integer> bashoIds, Collection<Integer> rikishiIds);

	// 관리자 반즈케 관리: 한 리키시가 그 바쇼에 이미 등록됐는지 (uq_rikishi_basho 제약과 동일 키)
	boolean existsByBashoEntityIdAndRikishiEntityId(Integer bashoId, Integer rikishiId);

	// sumo-api 반즈케 임포트 upsert용 (uq_rikishi_basho 제약과 동일 키 — 리키시는 바쇼당 1행)
	Optional<BanzukeEntity> findByBashoEntityIdAndRikishiEntityId(Integer bashoId, Integer rikishiId);

	// 관리자 바쇼 목록의 반즈케 등록 수
	long countByBashoEntityId(Integer bashoId);

	// 킨보시 파생용 - 그 바쇼 전체 반즈케(디비전 무관), 리키시 fetch.
	@Query("""
    SELECT b FROM BanzukeEntity b
    JOIN FETCH b.rikishiEntity
    WHERE b.bashoEntity.id = :bashoId
""")
	List<BanzukeEntity> findByBashoEntityIdFetchRikishi(@Param("bashoId") Integer bashoId);

	// 메인 페이지 "일문" 탭용 - 세키토리(마쿠우치+주료)만, 마쿠우치 먼저 그 안에서 순위순, 그다음 주료도 순위순.
	@Query("""
    SELECT b FROM BanzukeEntity b
    JOIN FETCH b.rikishiEntity r
    LEFT JOIN FETCH r.heyaEntity
    WHERE b.bashoEntity.id = :bashoId
      AND b.division IN (com.torikumilab.sumoarchive.domain.entity.constant.Division.Makuuchi,
                          com.torikumilab.sumoarchive.domain.entity.constant.Division.Juryo)
    ORDER BY
        CASE b.division WHEN com.torikumilab.sumoarchive.domain.entity.constant.Division.Makuuchi THEN 1 ELSE 2 END,
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
	List<BanzukeEntity> findSekitoriByBasho(@Param("bashoId") Integer bashoId);

}