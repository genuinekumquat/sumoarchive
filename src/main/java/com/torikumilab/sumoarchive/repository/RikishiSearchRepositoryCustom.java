package com.torikumilab.sumoarchive.repository;

import com.torikumilab.sumoarchive.domain.dto.search.RikishiSearchRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

//커스텀 메서드 명세
public interface RikishiSearchRepositoryCustom {
	Page<RikishiSearchRow> search(String keyword, Pageable pageable);
}