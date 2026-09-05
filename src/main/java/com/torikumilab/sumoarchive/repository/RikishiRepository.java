package com.torikumilab.sumoarchive.repository;

import com.torikumilab.sumoarchive.domain.entity.RikishiEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RikishiRepository extends JpaRepository<RikishiEntity, Integer>, RikishiSearchRepositoryCustom {

	// 관리자 리키시 목록(/admin/rikishi)용 - 공개 검색(RikishiSearchRepositoryCustom)과 달리
	// 시코나 이력까지 뒤지는 정교한 랭킹이 필요 없어서 단순 LIKE 검색으로 충분하다.
	Page<RikishiEntity> findByShikonaKrContainingOrShikonaJpContaining(String shikonaKr, String shikonaJp, Pageable pageable);
}
