package com.torikumilab.sumoarchive.repository;

import com.torikumilab.sumoarchive.domain.entity.HeyaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HeyaRepository extends JpaRepository<HeyaEntity, Integer> {
}