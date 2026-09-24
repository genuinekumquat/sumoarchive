package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.domain.dto.AwardImportResultDTO;
import com.torikumilab.sumoarchive.domain.dto.BanzukeImportResultDTO;
import com.torikumilab.sumoarchive.domain.dto.TorikumiImportResultDTO;
import com.torikumilab.sumoarchive.domain.entity.BashoEntity;
import com.torikumilab.sumoarchive.domain.entity.constant.Division;
import com.torikumilab.sumoarchive.repository.BashoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 특정 바쇼 또는 특정 연도의 전체 데이터(반즈케, 토리쿠미, 우승·삼상·킨보시)를
 * 마쿠우치 + 쥬료 2대 디비전 기준으로 일괄 적재(Batch Import)하는 서비스.
 */
@Service
@RequiredArgsConstructor
public class BashoBatchImportService {

	private static final Logger log = LoggerFactory.getLogger(BashoBatchImportService.class);

	private final BashoRepository bashoRepository;
	private final BanzukeImportService banzukeImportService;
	private final TorikumiImportService torikumiImportService;
	private final AwardImportService awardImportService;

	public record BashoBatchResult(
			String bashoLabel,
			int banzukeCreated,
			int banzukeUpdated,
			int torikumiCreated,
			int torikumiUpdated,
			int yushoCreated,
			int sanshoCreated,
			int kinboshiCreated,
			List<String> warnings
	) {}

	public record YearBatchResult(
			int year,
			int totalBashos,
			int totalTorikumiCreated,
			int totalBanzukeCreated,
			List<BashoBatchResult> bashoResults
	) {}

	/**
	 * 특정 바쇼 1건의 마쿠우치·쥬료 반즈케, 토리쿠미(1~15일차), 우승·삼상·킨보시를 일괄 동기화.
	 */
	@CacheEvict(value = {"banzuke", "bashoOptions", "ichimonStructure"}, allEntries = true)
	public BashoBatchResult importBashoFull(Integer bashoId) {
		BashoEntity basho = bashoRepository.findById(bashoId)
				.orElseThrow(() -> new EntityNotFoundException("바쇼를 찾을 수 없습니다. id=" + bashoId));

		String label = basho.getBashoYear() + "년 " + basho.getBashoMonth().getMonthValue() + "월 "
				+ basho.getBashoMonth().getDisplayNameKr();
		log.info("[BashoBatchImport] {} 일괄 임포트 시작 (Makuuchi + Juryo)", label);

		List<String> warnings = new ArrayList<>();
		int bzCreated = 0;
		int bzUpdated = 0;
		int tkCreated = 0;
		int tkUpdated = 0;

		// 1. 반즈케 (Makuuchi, Juryo)
		for (Division div : List.of(Division.Makuuchi, Division.Juryo)) {
			try {
				BanzukeImportResultDTO res = banzukeImportService.importDivision(bashoId, div);
				bzCreated += res.created();
				bzUpdated += res.updated();
				warnings.addAll(res.unmatched());
			} catch (Exception e) {
				warnings.add(div + " 반즈케 임포트 실패: " + e.getMessage());
			}
		}

		// 2. 토리쿠미 (Makuuchi, Juryo)
		for (Division div : List.of(Division.Makuuchi, Division.Juryo)) {
			try {
				TorikumiImportResultDTO res = torikumiImportService.importDivision(bashoId, div);
				tkCreated += res.created();
				tkUpdated += res.updated();
				warnings.addAll(res.skipped());
			} catch (Exception e) {
				warnings.add(div + " 토리쿠미 임포트 실패: " + e.getMessage());
			}
		}

		// 3. 우승·삼상·킨보시
		int yusho = 0, sansho = 0, kinboshi = 0;
		try {
			AwardImportResultDTO res = awardImportService.importForBasho(bashoId);
			yusho = res.yushoCreated();
			sansho = res.sanshoCreated();
			kinboshi = res.kinboshiCreated();
			warnings.addAll(res.skipped());
		} catch (Exception e) {
			warnings.add("수상/킨보시 임포트 실패: " + e.getMessage());
		}

		log.info("[BashoBatchImport] {} 완료 - 반즈케: 신규 {}/갱신 {}, 토리쿠미: 신규 {}/갱신 {}, 우승 {}, 삼상 {}, 킨보시 {}",
				label, bzCreated, bzUpdated, tkCreated, tkUpdated, yusho, sansho, kinboshi);

		return new BashoBatchResult(
				label, bzCreated, bzUpdated, tkCreated, tkUpdated, yusho, sansho, kinboshi, warnings
		);
	}

	/**
	 * 특정 연도(예: 2025)의 모든 바쇼를 순차적으로 일괄 임포트.
	 */
	@CacheEvict(value = {"banzuke", "bashoOptions", "ichimonStructure"}, allEntries = true)
	public YearBatchResult importYear(int year) {
		List<BashoEntity> bashos = bashoRepository.findByBashoYearOrderByStartDateAsc(year);
		log.info("[BashoBatchImport] {}년도 바쇼 총 {}개 일괄 임포트 시작", year, bashos.size());

		List<BashoBatchResult> results = new ArrayList<>();
		int totalBzCreated = 0;
		int totalTkCreated = 0;

		for (BashoEntity b : bashos) {
			BashoBatchResult r = importBashoFull(b.getId());
			results.add(r);
			totalBzCreated += r.banzukeCreated();
			totalTkCreated += r.torikumiCreated();
		}

		log.info("[BashoBatchImport] {}년도 일괄 임포트 완료 - 총 {}개 바쇼, 반즈케 신규 {}건, 경기 신규 {}건",
				year, bashos.size(), totalBzCreated, totalTkCreated);

		return new YearBatchResult(year, bashos.size(), totalTkCreated, totalBzCreated, results);
	}
}
