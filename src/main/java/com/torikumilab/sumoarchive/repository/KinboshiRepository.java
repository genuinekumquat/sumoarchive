package com.torikumilab.sumoarchive.repository;

import com.torikumilab.sumoarchive.domain.entity.KinboshiEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface KinboshiRepository extends JpaRepository<KinboshiEntity, Integer> {

	long countByWinnerRikishiEntityId(Integer rikishiId);

	// 프로필 「수상 내역」용 - 바쇼·일차(대전)와 진 요코즈나 이름까지 한 번에
	@Query("select k from KinboshiEntity k join fetch k.torikumiEntity t join fetch t.bashoEntity"
			+ " join fetch k.loserYokozuna where k.winnerRikishiEntity.id = :rikishiId")
	List<KinboshiEntity> findWithBoutByWinnerRikishiId(@Param("rikishiId") Integer rikishiId);

	// 킨보시 파생(derive) 재실행 시 중복 방지 - 한 대전당 킨보시는 최대 1건
	boolean existsByTorikumiEntityId(Integer torikumiId);
}
