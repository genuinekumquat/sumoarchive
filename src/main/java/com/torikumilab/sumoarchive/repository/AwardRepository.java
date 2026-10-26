package com.torikumilab.sumoarchive.repository;

import com.torikumilab.sumoarchive.domain.entity.AwardEntity;
import com.torikumilab.sumoarchive.domain.entity.constant.AwardType;
import com.torikumilab.sumoarchive.domain.entity.constant.Division;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * ⚠ 신규 파일 — 지금까지 DataSeeder 등에서 사용된 적이 없어서 새로 만들었습니다.
 * award 테이블 명세서(uq_award: rikishi_id+basho_id+division+award_type) 기준으로 작성.
 */
public interface AwardRepository extends JpaRepository<AwardEntity, Integer> {

	// 특정 선수의 특정 디비전 우승 횟수 (예: 마쿠우치 유쇼, 주료 유쇼)
	long countByRikishiEntityIdAndDivisionAndAwardType(Integer rikishiId, Division division, AwardType awardType);

	// 특정 선수의 산쇼(수훈상/감투상/기능상) 횟수 - 디비전 구분 없이 전체 합산
	long countByRikishiEntityIdAndAwardType(Integer rikishiId, AwardType awardType);

	// 프로필 「수상 내역」용 - 바쇼 라벨을 같이 쓰므로 바쇼까지 한 번에
	@Query("select a from AwardEntity a join fetch a.bashoEntity where a.rikishiEntity.id = :rikishiId")
	List<AwardEntity> findWithBashoByRikishiId(@Param("rikishiId") Integer rikishiId);

	// 임포트 upsert 사전 검사 (uq_award: rikishi_id + basho_id + division + award_type)
	boolean existsByRikishiEntityIdAndBashoEntityIdAndDivisionAndAwardType(
			Integer rikishiId, Integer bashoId, Division division, AwardType awardType);
}
