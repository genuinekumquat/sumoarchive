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
 * 6개 디비전 전부(마쿠우치~조노구치) 기준으로 일괄 적재(Batch Import)하는 서비스.
 * 하위 리그까지 받으므로 바쇼 1개에 1분 안팎 걸린다 (처음 보는 은퇴 선수가 많으면 단건 조회로 더 걸림).
 */
@Service
@RequiredArgsConstructor
public class BashoBatchImportService {

	private static final Logger log = LoggerFactory.getLogger(BashoBatchImportService.class);

	private final BashoRepository bashoRepository;
	private final BanzukeImportService banzukeImportService;
	private final TorikumiImportService torikumiImportService;
	private final AwardImportService awardImportService;
	private final RankHistorySyncService rankHistorySyncService;

	public record BashoBatchResult(
			String bashoLabel,
			int banzukeCreated,
			int banzukeUpdated,
			int torikumiCreated,
			int torikumiUpdated,
			int yushoCreated,
			int sanshoCreated,
			int kinboshiCreated,
			List<String> warnings,
			// 반즈케·토리쿠미·수상 단계 중 예외로 끝난 수 (경고에도 「… 임포트 실패」로 한 줄씩 들어간다).
			// 미매칭·미확정 같은 일반 경고와 구분해 자동 임포트 실패 알림(/status/auto-import)에 쓴다
			int failureCount
	) {}

	public record YearBatchResult(
			int year,
			int totalBashos,
			int totalTorikumiCreated,
			int totalBanzukeCreated,
			List<BashoBatchResult> bashoResults
	) {}

	/**
	 * 특정 바쇼 1건의 전 디비전 반즈케(공식 승·패·휴 포함), 토리쿠미(1~15일차), 우승·삼상·킨보시를 일괄 동기화.
	 */
	@CacheEvict(value = {"banzuke", "bashoOptions", "ichimonStructure"}, allEntries = true)
	public BashoBatchResult importBashoFull(Integer bashoId) {
		BashoEntity basho = bashoRepository.findById(bashoId)
				.orElseThrow(() -> new EntityNotFoundException("바쇼를 찾을 수 없습니다. id=" + bashoId));

		String label = basho.getBashoYear() + "년 " + basho.getBashoMonth().getMonthValue() + "월 "
				+ basho.getBashoMonth().getDisplayNameKr();
		log.info("[BashoBatchImport] {} 일괄 임포트 시작 (전 디비전)", label);

		List<String> warnings = new ArrayList<>();
		int failures = 0;
		int bzCreated = 0;
		int bzUpdated = 0;
		int tkCreated = 0;
		int tkUpdated = 0;

		// 1. 반즈케 (전 디비전)
		for (Division div : Division.values()) {
			try {
				BanzukeImportResultDTO res = banzukeImportService.importDivision(bashoId, div);
				bzCreated += res.created();
				bzUpdated += res.updated();
				warnings.addAll(res.unmatched());
			} catch (Exception e) {
				warnings.add(div + " 반즈케 임포트 실패: " + e.getMessage());
				failures++;
			}
		}

		// 2. 토리쿠미 (전 디비전)
		for (Division div : Division.values()) {
			try {
				TorikumiImportResultDTO res = torikumiImportService.importDivision(bashoId, div);
				tkCreated += res.created();
				tkUpdated += res.updated();
				warnings.addAll(res.skipped());
			} catch (Exception e) {
				warnings.add(div + " 토리쿠미 임포트 실패: " + e.getMessage());
				failures++;
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
			failures++;
		}

		// 4. 반즈케 변동용 계급 이력·은퇴 확인 (새 선수만 조회해서 보통 수 초). 선수 몇 명의 조회 실패는 경고로만
		try {
			RankHistorySyncService.SyncResult res = rankHistorySyncService.syncForBasho(bashoId);
			warnings.addAll(res.warnings());
		} catch (Exception e) {
			warnings.add("계급 이력·은퇴 확인 실패: " + e.getMessage());
			failures++;
		}

		log.info("[BashoBatchImport] {} 완료 - 반즈케: 신규 {}/갱신 {}, 토리쿠미: 신규 {}/갱신 {}, 우승 {}, 삼상 {}, 킨보시 {}",
				label, bzCreated, bzUpdated, tkCreated, tkUpdated, yusho, sansho, kinboshi);

		return new BashoBatchResult(
				label, bzCreated, bzUpdated, tkCreated, tkUpdated, yusho, sansho, kinboshi, warnings, failures
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
