package com.torikumilab.sumoarchive.repository;

import com.torikumilab.sumoarchive.domain.entity.RikishiShikonaHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RikishiShikonaHistoryRepository extends JpaRepository<RikishiShikonaHistoryEntity, Integer> {
	
	List<RikishiShikonaHistoryEntity> findByRikishiEntityIdOrderByValidFromAsc(Integer rikishiId);
}
