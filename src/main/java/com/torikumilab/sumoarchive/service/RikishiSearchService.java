package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.domain.dto.search.RikishiSearchResultDTO;
import com.torikumilab.sumoarchive.domain.dto.search.RikishiSearchRow;
import com.torikumilab.sumoarchive.repository.RikishiRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.Map;

@Service
public class RikishiSearchService {
	
	private static final int PAGE_SIZE = 15;
	
	private final RikishiRepository repository;
	private final HighestRankService highestRankService;

	public RikishiSearchService(RikishiRepository repository, HighestRankService highestRankService) {
		this.repository = repository;
		this.highestRankService = highestRankService;
	}
	
	public Page<RikishiSearchResultDTO> search(String keyword, int page) {
		if (keyword == null || keyword.isBlank()) {
			return Page.empty();
		}
		
		String normalized = keyword.strip();
		Pageable pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE);
		
		Page<RikishiSearchRow> rows = repository.search(normalized, pageable);
		// 최고위: 직접 입력값, 없으면 반즈케로 계산 (한 페이지 리키시를 한 번에)
		Map<Integer, HighestRankService.HighestRank> highest = highestRankService.resolveAll(rows.getContent().stream()
				.map(row -> new HighestRankService.Subject(row.rikishiId(), row.highestRank(),
						row.debutYear() != null ? YearMonth.of(row.debutYear(), 1) : null))
				.toList());
		return rows.map(row -> toDto(row, highest.get(row.rikishiId())));
	}
	
	private RikishiSearchResultDTO toDto(RikishiSearchRow row, HighestRankService.HighestRank highest) {
		return new RikishiSearchResultDTO(
				row.rikishiId(),
				row.name(),
				row.shikonaKr(),
				row.shikonaJp(),
				row.heyaName(),
				highest != null ? highest.rank() : null,
				formatPeriod(row.debutYear(), row.retirementYear()),
				determineStatus(row),
				row.oyakataNameKr(),
				row.heyaNameJp(),
				highest != null ? highest.rankJp() : null,
				formatPeriodJp(row.debutYear(), row.retirementYear()),
				row.oyakataNameJp()
		);
	}

	private String formatPeriodJp(Integer debut, Integer retirement) {
		if (debut == null) {
			return "-";
		}
		return retirement != null ? debut + " ~ " + retirement : debut + " ~ 現在";
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