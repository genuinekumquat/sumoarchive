package com.torikumilab.sumoarchive.repository;

import com.torikumilab.sumoarchive.domain.entity.BashoEntity;
import com.torikumilab.sumoarchive.domain.entity.constant.BashoMonth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BashoRepository extends JpaRepository<BashoEntity, Integer> {

	Optional<BashoEntity> findTopByStartDateLessThanEqualOrderByStartDateDesc(LocalDate today);

	// 메인 화면 기본 노출용 — 시작일이 아직 안 됐어도(반즈케 발표~본장소 개최 사이) 가장 최근 바쇼를 보여준다.
	// 단, 반즈케가 들어간 바쇼만. 바쇼만 먼저 등록되고(관리자 "바쇼 추가", 또는 sumo-api가 일정을 반즈케보다
	// 먼저 공개) 반즈케가 아직 없으면 메인이 "등록된 반즈케가 없습니다"로 바뀌는 것을 막는다.
	@Query("""
        SELECT b FROM BashoEntity b
        WHERE EXISTS (SELECT 1 FROM BanzukeEntity z WHERE z.bashoEntity = b)
        ORDER BY b.startDate DESC
    """)
	List<BashoEntity> findAllWithBanzukeOrderByStartDateDesc();

	default Optional<BashoEntity> findLatestWithBanzuke() {
		return findAllWithBanzukeOrderByStartDateDesc().stream().findFirst();
	}

	// 관리자 바쇼 목록용. bashoMonth는 @Enumerated(STRING)이라 그걸로 정렬하면 varchar 알파벳순이
	// 되어 월 순서가 깨진다. 생성 폼에서 시작일을 필수로 받으므로 startDate 정렬이 정확하다.
	List<BashoEntity> findAllByOrderByStartDateDesc();

	// 관리자 바쇼 생성 시 (연,월) 중복 사전 검사 (uq_basho 제약과 동일 키)
	boolean existsByBashoYearAndBashoMonth(Integer bashoYear, BashoMonth bashoMonth);

	// sumo-api 바쇼 임포트 매칭용
	Optional<BashoEntity> findByBashoYearAndBashoMonth(Integer bashoYear, BashoMonth bashoMonth);

	Optional<BashoEntity> findByExternalBashoId(String externalBashoId);

	List<BashoEntity> findByBashoYearOrderByStartDateAsc(Integer bashoYear);

	/**
	 * 리키시 상세 "기간 필터"용 - 두 바쇼 id → [이른 시작일, 늦은 시작일] (순서가 뒤바뀌어 와도 보정).
	 * 하나라도 null이거나 못 찾으면 null (= 통산).
	 */
	default LocalDate[] findStartDateRange(Integer fromBashoId, Integer toBashoId) {
		if (fromBashoId == null || toBashoId == null) {
			return null;
		}
		LocalDate d1 = findById(fromBashoId).map(BashoEntity::getStartDate).orElse(null);
		LocalDate d2 = findById(toBashoId).map(BashoEntity::getStartDate).orElse(null);
		if (d1 == null || d2 == null) {
			return null;
		}
		return d1.isBefore(d2) ? new LocalDate[]{d1, d2} : new LocalDate[]{d2, d1};
	}
}