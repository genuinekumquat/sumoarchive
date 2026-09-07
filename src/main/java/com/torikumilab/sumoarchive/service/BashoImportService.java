package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.client.SumoApiClient;
import com.torikumilab.sumoarchive.domain.dto.BashoImportResultDTO;
import com.torikumilab.sumoarchive.domain.dto.sumoapi.SumoApiBashoDTO;
import com.torikumilab.sumoarchive.domain.entity.BashoEntity;
import com.torikumilab.sumoarchive.domain.entity.constant.BashoMonth;
import com.torikumilab.sumoarchive.repository.BashoRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * sumo-api.com에서 바쇼 메타데이터를 가져온다. 본장소는 홀수달(1·3·5·7·9·11)에만 열리므로,
 * 요청 연도 범위 × 그 6개 월의 {@code YYYYMM}을 각각 조회해서 upsert한다.
 * API에 없는 조합은 조용히 스킵. 우승/삼상(yusho, specialPrizes)은 이번 범위 밖.
 */
@Service
@RequiredArgsConstructor
public class BashoImportService {

	private static final Logger log = LoggerFactory.getLogger(BashoImportService.class);

	/** 월 값 → BashoMonth enum */
	private static final Map<Integer, BashoMonth> MONTHS = Map.of(
			1, BashoMonth.JAN, 3, BashoMonth.MAR, 5, BashoMonth.MAY,
			7, BashoMonth.JUL, 9, BashoMonth.SEP, 11, BashoMonth.NOV);

	private final SumoApiClient sumoApiClient;
	private final BashoRepository bashoRepository;

	@Transactional
	public BashoImportResultDTO importRange(Integer fromYear, Integer toYear) {
		int nowYear = LocalDate.now().getYear();
		int from = fromYear != null ? fromYear : nowYear - 1;
		int to = toYear != null ? toYear : nowYear;
		if (from > to) {
			int tmp = from;
			from = to;
			to = tmp;
		}

		int created = 0;
		int updated = 0;
		List<String> skipped = new ArrayList<>();

		for (int year = from; year <= to; year++) {
			for (int month : new int[]{1, 3, 5, 7, 9, 11}) {
				String yyyymm = String.format("%04d%02d", year, month);
				SumoApiBashoDTO dto;
				try {
					dto = sumoApiClient.getBasho(yyyymm);
				} catch (RuntimeException e) {
					skipped.add(yyyymm + " (조회 실패)");
					continue;
				}
				LocalDate start = parseDate(dto == null ? null : dto.startDate());
				LocalDate end = parseDate(dto == null ? null : dto.endDate());
				// 미개최/미편성 바쇼는 Go 제로타임("0001-01-01T00:00:00Z")이 오거나 아예 비어 있다
				if (start == null || start.getYear() < 1900) {
					skipped.add(yyyymm + " (데이터 없음)");
					continue;
				}
				BashoMonth bm = MONTHS.get(month);

				BashoEntity existing = bashoRepository.findByBashoYearAndBashoMonth(year, bm).orElse(null);
				if (existing != null) {
					existing.updateFromApi(yyyymm, start, end);
					updated++;
				} else {
					bashoRepository.save(BashoEntity.builder()
							.bashoYear(year)
							.bashoMonth(bm)
							.startDate(start)
							.endDate(end)
							.externalBashoId(yyyymm)
							.build());
					created++;
				}
			}
		}

		log.info("[BashoImport] {}~{} - 신규 {}, 갱신 {}, 스킵 {}", from, to, created, updated, skipped.size());
		return new BashoImportResultDTO(from, to, created, updated, skipped);
	}

	private static LocalDate parseDate(String iso) {
		if (iso == null || iso.length() < 10) {
			return null;
		}
		try {
			return LocalDate.parse(iso.substring(0, 10));
		} catch (RuntimeException e) {
			return null;
		}
	}
}
