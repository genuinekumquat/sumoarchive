package com.torikumilab.sumoarchive.repository;

import com.torikumilab.sumoarchive.domain.entity.HeyaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HeyaRepository extends JpaRepository<HeyaEntity, Integer> {

	List<HeyaEntity> findAllByOrderByNameEnAsc();

	// 관리자가 저장해 nameKrAuto=false가 된 행은 제외한다(검수 완료).
	@Query("SELECT h FROM HeyaEntity h WHERE h.nameKrAuto IS NULL OR h.nameKrAuto = true ORDER BY h.nameEn ASC")
	List<HeyaEntity> findNameKrAutofillTargets();

	@Query("SELECT COUNT(h) FROM HeyaEntity h WHERE h.nameKrAuto IS NULL OR h.nameKrAuto = true")
	long countNameKrAutofillTargets();
}
