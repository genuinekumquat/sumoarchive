package com.torikumilab.sumoarchive.repository;

import com.torikumilab.sumoarchive.domain.entity.BashoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface BashoRepository extends JpaRepository<BashoEntity, Integer> {
	
	Optional<BashoEntity> findTopByStartDateLessThanEqualOrderByStartDateDesc(LocalDate today);
}