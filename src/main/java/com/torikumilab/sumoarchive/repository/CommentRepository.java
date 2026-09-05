package com.torikumilab.sumoarchive.repository;

import com.torikumilab.sumoarchive.domain.entity.CommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<CommentEntity, Integer> {

	// 특정 토리쿠미의 댓글 전체(삭제된 것 포함 - 화면에서 "삭제된 댓글입니다"로 레이아웃 유지)를 작성순으로.
	@Query("""
        SELECT c FROM CommentEntity c
        WHERE c.torikumiEntity.id = :torikumiId
        ORDER BY c.createdAt ASC, c.id ASC
    """)
	List<CommentEntity> findByTorikumiId(@Param("torikumiId") Integer torikumiId);
}
