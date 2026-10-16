package com.torikumilab.sumoarchive.repository;

import com.torikumilab.sumoarchive.domain.entity.FeedbackEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<FeedbackEntity, Integer> {

	// 관리자 의견함 - 최신순 (전체 / 미처리만)
	Page<FeedbackEntity> findAllByOrderByIdDesc(Pageable pageable);

	Page<FeedbackEntity> findByHandledFalseOrderByIdDesc(Pageable pageable);

	long countByHandledFalse();
}
