package com.torikumilab.sumoarchive.repository;

import com.torikumilab.sumoarchive.domain.dto.KimariteCountRow;
import com.torikumilab.sumoarchive.domain.entity.TorikumiEntity;
import com.torikumilab.sumoarchive.domain.entity.constant.Division;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TorikumiRepository extends JpaRepository<TorikumiEntity, Integer> {

	// 토리쿠미 상세 페이지용 - 바쇼/동서/승자를 한 방에 fetch (loser는 승자로 역산 가능해 생략).
	@Query("""
    SELECT t FROM TorikumiEntity t
    JOIN FETCH t.bashoEntity
    JOIN FETCH t.eastRikishiEntity
    JOIN FETCH t.westRikishiEntity
    LEFT JOIN FETCH t.winnerRikishiEntity
    WHERE t.id = :id
""")
	Optional<TorikumiEntity> findDetailById(@Param("id") Integer id);
	
	// 해당 선수가 마에가시라로서 요코즈나를 격파해 킨보시를 딴 횟수
	long countByWinnerRikishiEntityId(Integer rikishiId);
	
	
	// 승수 카운트 (커리어 레코드용, 결정전은 공식 기록에서 제외한다고 가정)
	long countByWinnerRikishiEntityIdAndIsExtraMatchFalse(Integer rikishiId);
	long countByLoserRikishiEntityIdAndIsExtraMatchFalse(Integer rikishiId);
	
	// 키마리테(결정기술) 분포 - 이 선수가 "이긴" 경기 기준 (노트에 적어주신 findKimariteStats 그대로 이름 맞춤)
	@Query("""
    SELECT t.kimarite AS kimarite, COUNT(t) AS cnt
    FROM TorikumiEntity t
    WHERE t.winnerRikishiEntity.id = :rikishiId
      AND t.kimarite IS NOT NULL
      AND t.isExtraMatch = false
    GROUP BY t.kimarite
    ORDER BY COUNT(t) DESC
""")
	List<KimariteCountRow> findKimariteStats(@Param("rikishiId") Integer rikishiId);
	
	
	// 호시토리표(대전표)용 - 특정 바쇼에서 이 리키시가 동/서 어느 쪽으로 출전했든 전부 day 오름차순으로.
	// 결정전(is_extra_match=true)은 다른 통계와 동일하게 여기서도 제외 (15일 정규 대전만).
	@Query("""
    SELECT t FROM TorikumiEntity t
    JOIN FETCH t.eastRikishiEntity
    JOIN FETCH t.westRikishiEntity
    WHERE t.bashoEntity.id = :bashoId
      AND (t.eastRikishiEntity.id = :rikishiId OR t.westRikishiEntity.id = :rikishiId)
      AND t.isExtraMatch = false
    ORDER BY t.day ASC
""")
	List<TorikumiEntity> findMatchHistory(@Param("rikishiId") Integer rikishiId, @Param("bashoId") Integer bashoId);

	// 킨보시 파생용 - 특정 바쇼의 정규 대전 전부, 승자/패자 fetch.
	@Query("""
    SELECT t FROM TorikumiEntity t
    LEFT JOIN FETCH t.winnerRikishiEntity
    LEFT JOIN FETCH t.loserRikishiEntity
    WHERE t.bashoEntity.id = :bashoId
      AND t.isExtraMatch = false
""")
	List<TorikumiEntity> findRegularByBasho(@Param("bashoId") Integer bashoId);

	// ===== 관리자 토리쿠미 관리 =====

	// 바쇼 목록의 대전 수 (디비전 무관)
	long countByBashoEntityId(Integer bashoId);

	// sumo-api 토리쿠미 임포트 upsert용 (external_id UNIQUE)
	Optional<TorikumiEntity> findByExternalId(String externalId);

	// uq_torikumi(basho_id, day, east_rikishi_id, west_rikishi_id, is_extra_match) 사전 검사
	boolean existsByBashoEntityIdAndDayAndEastRikishiEntityIdAndWestRikishiEntityIdAndIsExtraMatch(
			Integer bashoId, Integer day, Integer eastRikishiId, Integer westRikishiId, boolean isExtraMatch);

	// 관리자 대전 목록 - 특정 바쇼/날짜/디비전, 정규 → 결정전 순, id 순
	@Query("""
    SELECT t FROM TorikumiEntity t
    JOIN FETCH t.eastRikishiEntity
    JOIN FETCH t.westRikishiEntity
    LEFT JOIN FETCH t.winnerRikishiEntity
    WHERE t.bashoEntity.id = :bashoId
      AND t.day = :day
      AND t.division = :division
    ORDER BY t.isExtraMatch ASC, t.id ASC
""")
	List<TorikumiEntity> findForAdmin(@Param("bashoId") Integer bashoId,
									  @Param("day") Integer day,
									  @Param("division") Division division);

}