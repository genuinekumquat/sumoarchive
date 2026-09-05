package com.torikumilab.sumoarchive.repository;

import com.torikumilab.sumoarchive.domain.entity.RikishiEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RikishiRepository extends JpaRepository<RikishiEntity, Integer>, RikishiSearchRepositoryCustom {
	
	// 기존 리포지토리에 Custom 상속 추가
}
