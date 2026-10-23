package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.domain.dto.RosterImportResultDTO;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
 * <p>새 바쇼 등록(바쇼 메타 임포트)은 바쇼마다 한 번이라 자동화하지 않는다 — DEPLOYMENT 7절 1).
 * 명단(로스터) 동기화는 바쇼마다 첫 자동 회차에 한 번 같이 돌린다 - 이적·헤야 개명·폐업은 반즈케 발표 즈음 반영되는데
 * 관리자가 잊으면 헤야 소속이 계속 옛날로 남기 때문. 실패하면 다음 회차에 다시 시도한다.
 * {@code app.auto-import.enabled=true}일 때만 돌고(운영 프로필), 마지막 실행 결과는 메모리에만 두어
 * 관리자 바쇼 화면에 보여준다(재시작하면 사라짐).</p>
 */
@Service
public class BashoAutoImportScheduler {

	private static final Logger log = LoggerFactory.getLogger(BashoAutoImportScheduler.class);

	/** 바쇼 일정·경기 시각 기준 (컨테이너 시간대 Asia/Seoul과 같은 시각) */
	private static final ZoneId ZONE = ZoneId.of("Asia/Tokyo");

	/** 바쇼 기간에 이 시간 넘게 실행이 없으면 스케줄러가 멈춘 것으로 본다 */
	static final int STALE_HOURS = 15;

	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("MM-dd HH:mm");

	private final BashoRepository bashoRepository;
	private final BashoBatchImportService bashoBatchImportService;
	private final RosterImportService rosterImportService;
	private final boolean enabled;

	private final AtomicBoolean running = new AtomicBoolean(false);
	private volatile LastRun lastRun;
	private volatile RosterSync lastRosterSync;
	/** 이번 앱 실행에서 명단 동기화를 마친 바쇼 id. 재시작하면 비어서 한 번 더 도는데, 여러 번 돌려도 안전하다. */
	private final Set<Integer> rosterSyncedBashoIds = ConcurrentHashMap.newKeySet();

	public BashoAutoImportScheduler(BashoRepository bashoRepository,
									BashoBatchImportService bashoBatchImportService,
									RosterImportService rosterImportService,
									@Value("${app.auto-import.enabled:false}") boolean enabled) {
		this.bashoRepository = bashoRepository;
		this.bashoBatchImportService = bashoBatchImportService;
		this.rosterImportService = rosterImportService;
		this.enabled = enabled;
	}

	/** 관리자 화면용: 마지막 명단 동기화. result는 성공했을 때만, error는 실패했을 때만 있다. */
	public record RosterSync(LocalDateTime finishedAt, RosterImportResultDTO result, String error) {}

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

		/** 디비전·수상 단계 중 예외로 끝난 수 (미매칭·미확정 같은 일반 경고는 세지 않는다) */
		public int failureCount() {
			return results.stream().mapToInt(BashoBatchImportService.BashoBatchResult::failureCount).sum();
		}

		public boolean failed() {
			return error != null || failureCount() > 0;
		}
	}

	public record Status(boolean enabled, boolean running, LastRun lastRun, RosterSync lastRosterSync) {}

	public Status status() {
		return new Status(enabled, running.get(), lastRun, lastRosterSync);
	}

	/** 외부 감시(UptimeRobot)용 판정. ok=false면 /status/auto-import가 503을 돌려준다. */
	public record Alert(boolean ok, String message) {}

	/**
	 * 마지막 실행이 실패했거나, 바쇼 기간인데 {@value #STALE_HOURS}시간 넘게 실행이 없으면(스케줄러 멈춤) 실패.
	 * 회차 사이 가장 긴 간격은 9:00 → 18:30(9시간 반)이라 여유를 두었다.
	 * 앱을 재시작하면 lastRun이 비어서 다음 회차까지는 확인할 수 없다 → 그동안은 정상으로 본다.
	 */
	public Alert alert(LocalDateTime now) {
		if (!enabled) {
			return new Alert(true, "OK - 자동 임포트 꺼짐");
		}
		RosterSync roster = lastRosterSync;
		if (roster != null && roster.error() != null) {
			return new Alert(false, "FAIL - 명단 동기화 " + roster.finishedAt().format(TIME) + " 실패 — 관리자 바쇼 화면 확인");
		}
		LastRun run = lastRun;
		if (run != null && run.failed()) {
			return new Alert(false, "FAIL - 마지막 실행 " + run.finishedAt().format(TIME) + " 실패 "
					+ (run.error() != null ? "(오류)" : run.failureCount() + "건") + " — 관리자 바쇼 화면 확인");
		}
		boolean due = bashoRepository.findAllByOrderByStartDateDesc().stream()
				.anyMatch(b -> isDue(b, now.toLocalDate()));
		if (due && run != null && run.finishedAt().isBefore(now.minusHours(STALE_HOURS))) {
			return new Alert(false, "FAIL - 바쇼 기간인데 " + STALE_HOURS + "시간 넘게 실행 없음 (마지막 "
					+ run.finishedAt().format(TIME) + ")");
		}
		if (run == null) {
			return new Alert(true, "OK - 앱 시작 후 실행 기록 없음" + (due ? " (다음 회차 대기)" : ""));
		}
		return new Alert(true, "OK - 마지막 실행 " + run.finishedAt().format(TIME));
	}

	public Alert alert() {
		return alert(LocalDateTime.now(ZONE));
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
			syncRosterOnce(targets.get(0)); // 가장 최근 대상 바쇼 기준 한 번
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

	/**
	 * 그 바쇼에서 아직 명단 동기화를 안 했으면 sumo-api 현역 명단으로 소속 헤야 등을 갱신한다.
	 * 실패해도 경기 임포트는 계속하고, 표시만 남겨 다음 회차에 다시 시도한다(알림도 FAIL).
	 * 새 헤야(신설·개명)가 생기면 관리자 헤야 관리에서 일본어명·일문을 채워야 일문 탭에 나온다.
	 */
	private void syncRosterOnce(BashoEntity basho) {
		if (rosterSyncedBashoIds.contains(basho.getId())) {
			return;
		}
		try {
			RosterImportResultDTO result = rosterImportService.importRoster();
			rosterSyncedBashoIds.add(basho.getId());
			lastRosterSync = new RosterSync(LocalDateTime.now(ZONE), result, null);
			log.info("[AutoImport] 명단 동기화 - 헤야 신규 {}개, 리키시 신규 {}명 / 갱신 {}명",
					result.heyaCreated(), result.rikishiCreated(), result.rikishiUpdated());
		} catch (Exception e) {
			String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
			lastRosterSync = new RosterSync(LocalDateTime.now(ZONE), null, message);
			log.error("[AutoImport] 명단 동기화 실패 (다음 회차에 재시도)", e);
		}
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
