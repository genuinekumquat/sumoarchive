package com.torikumilab.sumoarchive.repository;

import com.torikumilab.sumoarchive.domain.entity.KinboshiEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KinboshiRepository extends JpaRepository<KinboshiEntity, Integer> {
	
	long countByWinnerRikishiEntityId(Integer rikishiId);

	// 킨보시 파생(derive) 재실행 시 중복 방지 - 한 대전당 킨보시는 최대 1건
	boolean existsByTorikumiEntityId(Integer torikumiId);
}