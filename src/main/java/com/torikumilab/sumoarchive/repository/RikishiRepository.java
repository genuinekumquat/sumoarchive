package com.torikumilab.sumoarchive.repository;

import com.torikumilab.sumoarchive.domain.entity.RikishiEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RikishiRepository extends JpaRepository<RikishiEntity, Integer>, RikishiSearchRepositoryCustom {

	// 관리자 리키시 목록(/admin/rikishi)용 - 공개 검색(RikishiSearchRepositoryCustom)과 달리
	// 시코나 이력까지 뒤지는 정교한 랭킹이 필요 없어서 단순 LIKE 검색으로 충분하다.
	// sumo-api 임포트 이후 한국어 시코나가 아직 없는 리키시가 많아 로마자(shikonaEn)도 함께 매칭한다.
	Page<RikishiEntity> findByShikonaKrContainingOrShikonaJpContainingOrShikonaEnContaining(
			String shikonaKr, String shikonaJp, String shikonaEn, Pageable pageable);

	// sumo-api 매칭/재임포트용
	Optional<RikishiEntity> findByExternalApiId(Integer externalApiId);
}
