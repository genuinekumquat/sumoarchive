package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.domain.entity.BashoEntity;
import com.torikumilab.sumoarchive.repository.BashoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 진행 중인 바쇼를 매일 저녁 자동으로 일괄 임포트한다 (관리자의 "⚡ 전체 일괄 임포트 →"와 같은 작업).
 *
 * <p>마쿠우치 경기가 18시 전후에 끝나므로 18:30·20:00·22:00에 받고, 밤사이 정정이 있을 수 있어
 * 다음 날 9:00에 한 번 더 받는다. 대상은 "시작일 - 2일 ≤ 오늘 ≤ 종료일 + 1일"인 바쇼뿐이라 바쇼 기간이 아니면
 * 아무것도 하지 않는다. 개막 이틀 전부터 받는 건 첫날·둘째 날 대진이 그때 발표되기 때문(예정 대진, V6),
 * 종료 다음 날까지 받는 건 결정전·수상이 폐막 후에 반영되기 때문.
 * 임포트는 여러 번 돌려도 안전하다(있는 건 갱신, 결과 미확정 경기는 예정 대진으로).</p>
 *
 * <p>새 바쇼 등록(로스터·바쇼 메타 임포트)은 바쇼마다 한 번이라 자동화하지 않는다 — DEPLOYMENT 7절 1).
 * {@code app.auto-import.enabled=true}일 때만 돌고(운영 프로필), 마지막 실행 결과는 메모리에만 두어
 * 관리자 바쇼 화면에 보여준다(재시작하면 사라짐).</p>
 */
@Service
public class BashoAutoImportScheduler {

	private static final Logger log = LoggerFactory.getLogger(BashoAutoImportScheduler.class);

	/** 바쇼 일정·경기 시각 기준 (컨테이너 시간대 Asia/Seoul과 같은 시각) */
	private static final ZoneId ZONE = ZoneId.of("Asia/Tokyo");

	private final BashoRepository bashoRepository;
	private final BashoBatchImportService bashoBatchImportService;
	private final boolean enabled;

	private final AtomicBoolean running = new AtomicBoolean(false);
	private volatile LastRun lastRun;

	public BashoAutoImportScheduler(BashoRepository bashoRepository,
									BashoBatchImportService bashoBatchImportService,
									@Value("${app.auto-import.enabled:false}") boolean enabled) {
		this.bashoRepository = bashoRepository;
		this.bashoBatchImportService = bashoBatchImportService;
		this.enabled = enabled;
	}

	/** 관리자 화면용: 마지막으로 대상 바쇼가 있어 실제로 임포트한 실행. error가 있으면 results는 그 전까지의 것. */
	public record LastRun(
			LocalDateTime startedAt,
			LocalDateTime finishedAt,
			List<BashoBatchImportService.BashoBatchResult> results,
			String error
	) {
		public int torikumiCreated() {
			return results.stream().mapToInt(BashoBatchImportService.BashoBatchResult::torikumiCreated).sum();
		}

		public int warningCount() {
			return results.stream().mapToInt(r -> r.warnings().size()).sum();
		}
	}

	public record Status(boolean enabled, boolean running, LastRun lastRun) {}

	public Status status() {
		return new Status(enabled, running.get(), lastRun);
	}

	@Scheduled(cron = "0 30 18 * * *", zone = "Asia/Tokyo")
	@Scheduled(cron = "0 0 9,20,22 * * *", zone = "Asia/Tokyo")
	public void runScheduled() {
		if (!enabled) {
			return;
		}
		runIfDue(LocalDate.now(ZONE));
	}

	/** 오늘 받을 바쇼가 있으면 일괄 임포트. 이전 실행이 아직 돌고 있으면 이번 회차는 건너뛴다. */
	void runIfDue(LocalDate today) {
		List<BashoEntity> targets = bashoRepository.findAllByOrderByStartDateDesc().stream()
				.filter(b -> isDue(b, today))
				.toList();
		if (targets.isEmpty()) {
			log.debug("[AutoImport] {} 대상 바쇼 없음 (바쇼 기간 아님)", today);
			return;
		}
		if (!running.compareAndSet(false, true)) {
			log.warn("[AutoImport] 이전 자동 임포트가 아직 진행 중이라 이번 회차는 건너뜀");
			return;
		}

		LocalDateTime startedAt = LocalDateTime.now(ZONE);
		List<BashoBatchImportService.BashoBatchResult> results = new ArrayList<>();
		String error = null;
		try {
			for (BashoEntity basho : targets) {
				results.add(bashoBatchImportService.importBashoFull(basho.getId()));
			}
		} catch (Exception e) {
			error = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
			log.error("[AutoImport] 자동 임포트 실패", e);
		} finally {
			lastRun = new LastRun(startedAt, LocalDateTime.now(ZONE), List.copyOf(results), error);
			running.set(false);
		}
		log.info("[AutoImport] 완료 - 바쇼 {}개, 경기 신규 {}건, 경고 {}건{}",
				results.size(), lastRun.torikumiCreated(), lastRun.warningCount(), error != null ? ", 오류: " + error : "");
	}

	/** 시작일 - 2일 ≤ 오늘 ≤ 종료일 + 1일. 일정이 비어 있는 바쇼는 대상이 아니다. */
	static boolean isDue(BashoEntity basho, LocalDate today) {
		LocalDate start = basho.getStartDate();
		LocalDate end = basho.getEndDate();
		if (start == null || end == null) {
			return false;
		}
		return !today.isBefore(start.minusDays(2)) && !today.isAfter(end.plusDays(1));
	}
}
