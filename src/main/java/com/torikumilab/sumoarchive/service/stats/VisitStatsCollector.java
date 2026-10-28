package com.torikumilab.sumoarchive.service.stats;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 공개 페이지 방문을 메모리에서 세다가 1분마다 날짜별 합계로 DB에 더한다(요청마다 DB에 쓰지 않음).
 *
 * <p>순방문자: "그날의 랜덤 값 + IP + 브라우저"의 해시를 그날 동안만 메모리 Set에 두고, 처음 보는 해시면 1명 더한다.
 * 날짜가 바뀌면 랜덤 값과 Set을 버려 다음 날엔 같은 사람인지 알 수 없다. IP·브라우저 정보는 DB에 저장하지 않는다.
 * 앱을 재시작하면 Set이 비워져 그날은 같은 사람이 한 번 더 세질 수 있다(배포한 날만 조금 많게).</p>
 *
 * <p>DB에 쓰다 실패하면 그 숫자를 다음 회차에 다시 더한다. 정상 종료 시(@PreDestroy)에도 한 번 쓴다.</p>
 */
@Service
public class VisitStatsCollector {

	private static final Logger log = LoggerFactory.getLogger(VisitStatsCollector.class);

	static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
	/** 하루 순방문자 Set 상한 - 넘으면 순방문자만 더 세지 않는다(메모리 보호) */
	private static final int MAX_VISITORS_PER_DAY = 200_000;

	private final JdbcTemplate jdbcTemplate;
	private final SecureRandom random = new SecureRandom();

	/** record는 읽기 잠금(동시에 여럿), flush의 버킷 교체는 쓰기 잠금 - 교체 순간에 더한 숫자를 잃지 않게 */
	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private Buckets buckets = new Buckets();
	private volatile DayVisitors dayVisitors;

	public VisitStatsCollector(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	record PageKey(LocalDate date, String pageType, int targetId) {}

	record RefKey(LocalDate date, String source) {}

	record BookmarkKey(LocalDate date, int rikishiId) {}

	/** 다음 flush 때 DB에 더할 숫자들 */
	static final class Buckets {
		final Map<LocalDate, LongAdder> views = new ConcurrentHashMap<>();
		final Map<LocalDate, LongAdder> visitors = new ConcurrentHashMap<>();
		final Map<LocalDate, LongAdder> mobileVisitors = new ConcurrentHashMap<>();
		final Map<PageKey, LongAdder> pages = new ConcurrentHashMap<>();
		final Map<RefKey, LongAdder> refs = new ConcurrentHashMap<>();
		final Map<BookmarkKey, LongAdder> bookmarkAdds = new ConcurrentHashMap<>();
		final Map<BookmarkKey, LongAdder> bookmarkRemoves = new ConcurrentHashMap<>();

		boolean isEmpty() {
			return views.isEmpty() && visitors.isEmpty() && pages.isEmpty() && refs.isEmpty()
					&& bookmarkAdds.isEmpty() && bookmarkRemoves.isEmpty();
		}

		void addAll(Buckets other) {
			other.views.forEach((k, v) -> add(views, k, v.sum()));
			other.visitors.forEach((k, v) -> add(visitors, k, v.sum()));
			other.mobileVisitors.forEach((k, v) -> add(mobileVisitors, k, v.sum()));
			other.pages.forEach((k, v) -> add(pages, k, v.sum()));
			other.refs.forEach((k, v) -> add(refs, k, v.sum()));
			other.bookmarkAdds.forEach((k, v) -> add(bookmarkAdds, k, v.sum()));
			other.bookmarkRemoves.forEach((k, v) -> add(bookmarkRemoves, k, v.sum()));
		}

		static <K> void add(Map<K, LongAdder> map, K key, long n) {
			map.computeIfAbsent(key, k -> new LongAdder()).add(n);
		}
	}

	/** 그날의 랜덤 값과 이미 센 방문자 해시 */
	private record DayVisitors(LocalDate date, byte[] salt, Set<String> seen) {}

	/**
	 * 페이지뷰 1건. 봇·대상 아닌 페이지 판별은 호출하는 쪽(PageViewApiController)이 한다.
	 *
	 * @param referrerSource 외부 유입처(VisitClassifier.referrerSource), 없으면 null
	 */
	public void record(VisitClassifier.Page page, String clientIp, String userAgent, String referrerSource) {
		record(page, clientIp, userAgent, referrerSource, LocalDate.now(ZONE));
	}

	void record(VisitClassifier.Page page, String clientIp, String userAgent, String referrerSource, LocalDate today) {
		boolean newVisitor = markVisitor(today, clientIp, userAgent);
		lock.readLock().lock();
		try {
			Buckets b = buckets;
			Buckets.add(b.views, today, 1);
			Buckets.add(b.pages, new PageKey(today, page.type().name(), page.targetId()), 1);
			if (referrerSource != null) {
				Buckets.add(b.refs, new RefKey(today, referrerSource), 1);
			}
			if (newVisitor) {
				Buckets.add(b.visitors, today, 1);
				if (VisitClassifier.isMobile(userAgent)) {
					Buckets.add(b.mobileVisitors, today, 1);
				}
			}
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * 즐겨찾기 추가/해제 1건. 같은 날 같은 방문자(IP+브라우저)의 같은 리키시·같은 동작은 한 번만 센다
	 * (☆를 여러 번 눌러 숫자를 부풀리지 못하게). 리키시 존재·봇 판별은 호출하는 쪽(BookmarkStatApiController)이 한다.
	 */
	public void recordBookmark(int rikishiId, boolean added, String clientIp, String userAgent) {
		recordBookmark(rikishiId, added, clientIp, userAgent, LocalDate.now(ZONE));
	}

	void recordBookmark(int rikishiId, boolean added, String clientIp, String userAgent, LocalDate today) {
		if (!markSeen(today, clientIp + "|" + userAgent + "|bookmark|" + rikishiId + "|" + added)) {
			return;
		}
		lock.readLock().lock();
		try {
			Buckets b = buckets;
			Buckets.add(added ? b.bookmarkAdds : b.bookmarkRemoves, new BookmarkKey(today, rikishiId), 1);
		} finally {
			lock.readLock().unlock();
		}
	}

	private boolean markVisitor(LocalDate today, String clientIp, String userAgent) {
		return markSeen(today, clientIp + "|" + userAgent);
	}

	/** 그날 처음 보는 값이면 true. 값은 그날의 랜덤 값으로 해시해서만 메모리에 둔다. */
	private boolean markSeen(LocalDate today, String value) {
		DayVisitors day = dayVisitors;
		if (day == null || !day.date().equals(today)) {
			synchronized (this) {
				day = dayVisitors;
				if (day == null || !day.date().equals(today)) {
					byte[] salt = new byte[32];
					random.nextBytes(salt);
					day = new DayVisitors(today, salt, ConcurrentHashMap.newKeySet());
					dayVisitors = day;
				}
			}
		}
		if (day.seen().size() >= MAX_VISITORS_PER_DAY) {
			return false;
		}
		return day.seen().add(hash(day.salt(), value));
	}

	private static String hash(byte[] salt, String value) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(salt);
			byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest, 0, 16);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	@Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
	public void flush() {
		Buckets old;
		lock.writeLock().lock();
		try {
			if (buckets.isEmpty()) {
				return;
			}
			old = buckets;
			buckets = new Buckets();
		} finally {
			lock.writeLock().unlock();
		}
		try {
			write(old);
		} catch (Exception e) {
			log.warn("[VisitStats] DB 저장 실패 - 다음 회차에 다시 더함: {}", e.getMessage());
			lock.readLock().lock();
			try {
				buckets.addAll(old);
			} finally {
				lock.readLock().unlock();
			}
		}
	}

	@PreDestroy
	public void flushOnShutdown() {
		flush();
	}

	private void write(Buckets b) {
		List<Object[]> site = new ArrayList<>();
		for (LocalDate d : union(b.views.keySet(), b.visitors.keySet())) {
			site.add(new Object[]{Date.valueOf(d), sum(b.visitors, d), sum(b.views, d), sum(b.mobileVisitors, d)});
		}
		if (!site.isEmpty()) jdbcTemplate.batchUpdate("""
				INSERT INTO site_daily_stat (stat_date, visitors, page_views, mobile_visitors) VALUES (?, ?, ?, ?) AS n
				ON DUPLICATE KEY UPDATE visitors = site_daily_stat.visitors + n.visitors,
				  page_views = site_daily_stat.page_views + n.page_views,
				  mobile_visitors = site_daily_stat.mobile_visitors + n.mobile_visitors""", site);

		List<Object[]> pages = new ArrayList<>();
		b.pages.forEach((k, v) -> pages.add(new Object[]{Date.valueOf(k.date()), k.pageType(), k.targetId(), v.sum()}));
		if (!pages.isEmpty()) jdbcTemplate.batchUpdate("""
				INSERT INTO page_daily_stat (stat_date, page_type, target_id, views) VALUES (?, ?, ?, ?) AS n
				ON DUPLICATE KEY UPDATE views = page_daily_stat.views + n.views""", pages);

		List<Object[]> refs = new ArrayList<>();
		b.refs.forEach((k, v) -> refs.add(new Object[]{Date.valueOf(k.date()), k.source(), v.sum()}));
		if (!refs.isEmpty()) jdbcTemplate.batchUpdate("""
				INSERT INTO referrer_daily_stat (stat_date, source, visits) VALUES (?, ?, ?) AS n
				ON DUPLICATE KEY UPDATE visits = referrer_daily_stat.visits + n.visits""", refs);

		List<Object[]> bookmarks = new ArrayList<>();
		Set<BookmarkKey> bookmarkKeys = new java.util.HashSet<>(b.bookmarkAdds.keySet());
		bookmarkKeys.addAll(b.bookmarkRemoves.keySet());
		for (BookmarkKey k : bookmarkKeys) {
			bookmarks.add(new Object[]{Date.valueOf(k.date()), k.rikishiId(), sum(b.bookmarkAdds, k), sum(b.bookmarkRemoves, k)});
		}
		if (!bookmarks.isEmpty()) jdbcTemplate.batchUpdate("""
				INSERT INTO bookmark_daily_stat (stat_date, rikishi_id, adds, removes) VALUES (?, ?, ?, ?) AS n
				ON DUPLICATE KEY UPDATE adds = bookmark_daily_stat.adds + n.adds,
				  removes = bookmark_daily_stat.removes + n.removes""", bookmarks);
	}

	private static Set<LocalDate> union(Set<LocalDate> a, Set<LocalDate> b) {
		Set<LocalDate> all = new java.util.TreeSet<>(a);
		all.addAll(b);
		return all;
	}

	private static <K> long sum(Map<K, LongAdder> map, K key) {
		LongAdder v = map.get(key);
		return v == null ? 0 : v.sum();
	}
}
