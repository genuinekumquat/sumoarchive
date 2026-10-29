package com.torikumilab.sumoarchive.repository;

import com.torikumilab.sumoarchive.domain.entity.RikishiShikonaHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface RikishiShikonaHistoryRepository extends JpaRepository<RikishiShikonaHistoryEntity, Integer> {

	List<RikishiShikonaHistoryEntity> findByRikishiEntityIdOrderByValidFromAsc(Integer rikishiId);

	// 지난 경기·반즈케의 「당시 시코나」용 - 화면에 나오는 리키시들 이력을 한 번에 (ShikonaAtBasho)
	List<RikishiShikonaHistoryEntity> findByRikishiEntityIdIn(Collection<Integer> rikishiIds);
}
