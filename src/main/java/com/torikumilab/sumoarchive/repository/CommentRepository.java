package com.torikumilab.sumoarchive.repository;

import com.torikumilab.sumoarchive.domain.entity.CommentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

	// 관리자 댓글 관리 대시보드(/admin/comments)용 - 전체 사이트 댓글을 최신순으로, 경기 정보까지 한 번에 조회.
	@Query(
			value = """
        SELECT c FROM CommentEntity c
        JOIN FETCH c.torikumiEntity t
        JOIN FETCH t.bashoEntity
        JOIN FETCH t.eastRikishiEntity
        JOIN FETCH t.westRikishiEntity
        ORDER BY c.createdAt DESC, c.id DESC
    """,
			countQuery = "SELECT COUNT(c) FROM CommentEntity c"
	)
	Page<CommentEntity> findAllForAdmin(Pageable pageable);
}
