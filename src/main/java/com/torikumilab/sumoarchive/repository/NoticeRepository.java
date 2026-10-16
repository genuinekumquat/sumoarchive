package com.torikumilab.sumoarchive.repository;

import com.torikumilab.sumoarchive.domain.entity.NoticeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NoticeRepository extends JpaRepository<NoticeEntity, Integer> {

	// 메인 팝업 - 켜진 공지 중 가장 최근 것
	Optional<NoticeEntity> findFirstByActiveTrueOrderByIdDesc();

	// 관리자 목록 - 최신순
	List<NoticeEntity> findAllByOrderByIdDesc();
}
