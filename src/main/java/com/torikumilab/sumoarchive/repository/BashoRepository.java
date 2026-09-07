package com.torikumilab.sumoarchive.repository;

import com.torikumilab.sumoarchive.domain.entity.BashoEntity;
import com.torikumilab.sumoarchive.domain.entity.constant.BashoMonth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BashoRepository extends JpaRepository<BashoEntity, Integer> {

	Optional<BashoEntity> findTopByStartDateLessThanEqualOrderByStartDateDesc(LocalDate today);

	// 메인 화면 기본 노출용 — 시작일이 아직 안 됐어도(반즈케 발표~본장소 개최 사이) 가장 최근 바쇼를 보여준다.
	Optional<BashoEntity> findTopByOrderByStartDateDesc();

	// 관리자 바쇼 목록용. bashoMonth는 @Enumerated(STRING)이라 그걸로 정렬하면 varchar 알파벳순이
	// 되어 월 순서가 깨진다. 생성 폼에서 시작일을 필수로 받으므로 startDate 정렬이 정확하다.
	List<BashoEntity> findAllByOrderByStartDateDesc();

	// 관리자 바쇼 생성 시 (연,월) 중복 사전 검사 (uq_basho 제약과 동일 키)
	boolean existsByBashoYearAndBashoMonth(Integer bashoYear, BashoMonth bashoMonth);

	// sumo-api 바쇼 임포트 매칭용
	Optional<BashoEntity> findByBashoYearAndBashoMonth(Integer bashoYear, BashoMonth bashoMonth);

	Optional<BashoEntity> findByExternalBashoId(String externalBashoId);
}