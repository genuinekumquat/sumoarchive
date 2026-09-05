package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.domain.dto.search.RikishiSearchResultDTO;
import com.torikumilab.sumoarchive.domain.dto.search.RikishiSearchRow;
import com.torikumilab.sumoarchive.repository.RikishiRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class RikishiSearchService {
	
	private static final int PAGE_SIZE = 15;
	
	private final RikishiRepository repository;
	
	public RikishiSearchService(RikishiRepository repository) {
		this.repository = repository;
	}
	
	public Page<RikishiSearchResultDTO> search(String keyword, int page) {
		if (keyword == null || keyword.isBlank()) {
			return Page.empty();
		}
		
		String normalized = keyword.strip();
		Pageable pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE);
		
		Page<RikishiSearchRow> rows = repository.search(normalized, pageable);
		return rows.map(this::toDto);
	}
	
	private RikishiSearchResultDTO toDto(RikishiSearchRow row) {
		return new RikishiSearchResultDTO(
				row.rikishiId(),
				row.name(),
				row.shikonaKr(),
				row.shikonaJp(),
				row.heyaName(),
				row.highestRank(),
				formatPeriod(row.debutYear(), row.retirementYear()),
				determineStatus(row),
				row.oyakataNameKr()
		);
	}
	
	private String determineStatus(RikishiSearchRow row) {
		if (Boolean.TRUE.equals(row.isActive())) {
			return "현역";
		}
		if (row.oyakataNameKr() != null && !row.oyakataNameKr().isBlank()) {
			return "오야카타";
		}
		return "은퇴";
	}
	
	private String formatPeriod(Integer debut, Integer retirement) {
		if (debut == null) {
			return "-";
		}
		return retirement != null ? debut + " ~ " + retirement : debut + " ~ 현재";
	}
}