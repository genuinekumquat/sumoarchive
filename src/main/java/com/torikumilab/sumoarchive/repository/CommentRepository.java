package com.torikumilab.sumoarchive.repository;

import com.torikumilab.sumoarchive.domain.entity.CommentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

	// 관리자 대시보드 "신고된 댓글" 필터 - 아직 처리(블라인드·본인삭제) 안 된 신고 댓글을 신고 많은 순, 최근 신고 순으로.
	@Query(
			value = """
        SELECT c FROM CommentEntity c
        JOIN FETCH c.torikumiEntity t
        JOIN FETCH t.bashoEntity
        JOIN FETCH t.eastRikishiEntity
        JOIN FETCH t.westRikishiEntity
        WHERE c.reportCount > 0 AND c.isDeleted = false
        ORDER BY c.reportCount DESC, c.lastReportedAt DESC, c.id DESC
    """,
			countQuery = "SELECT COUNT(c) FROM CommentEntity c WHERE c.reportCount > 0 AND c.isDeleted = false"
	)
	Page<CommentEntity> findReportedForAdmin(Pageable pageable);

	// 관리자 메뉴에 띄울 "처리 대기 중인 신고 댓글" 수.
	@Query("SELECT COUNT(c) FROM CommentEntity c WHERE c.reportCount > 0 AND c.isDeleted = false")
	long countPendingReported();

	// 신고 1건 반영. 읽고-더하고-저장하면 동시 신고 때 한 건이 사라질 수 있어 DB에서 바로 +1 한다.
	@Modifying
	@Query("UPDATE CommentEntity c SET c.reportCount = c.reportCount + 1, c.lastReportedAt = :now WHERE c.id = :id")
	int incrementReportCount(@Param("id") Integer id, @Param("now") LocalDateTime now);
}
