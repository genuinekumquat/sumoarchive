package com.torikumilab.sumoarchive.repository;

import com.torikumilab.sumoarchive.domain.entity.KinboshiEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KinboshiRepository extends JpaRepository<KinboshiEntity, Integer> {
	
	long countByWinnerRikishiEntityId(Integer rikishiId);
}