package com.torikumilab.sumoarchive.service.stats;

import com.torikumilab.sumoarchive.domain.entity.RikishiEntity;
import com.torikumilab.sumoarchive.repository.RikishiRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 관리자 방문 통계 화면(/admin/stats)용 조회. VisitStatsCollector가 1분마다 쌓은 날짜별 합계를 읽는다.
 */
@Service
public class VisitStatsService {

	public static final List<Integer> PERIODS = List.of(7, 30, 90);

	private final JdbcTemplate jdbcTemplate;
	private final RikishiRepository rikishiRepository;

	public VisitStatsService(JdbcTemplate jdbcTemplate, RikishiRepository rikishiRepository) {
		this.jdbcTemplate = jdbcTemplate;
		this.rikishiRepository = rikishiRepository;
	}

	public record Day(LocalDate date, int visitors, int pageViews, int mobileVisitors) {}

	public record Ranked(String label, Integer rikishiId, long count, int percent) {}

	/** 즐겨찾기 많은 리키시: net = 집계 시작 후 누적(추가 − 해제), periodAdds = 고른 기간에 새로 추가된 횟수 */
	public record Bookmarked(String label, Integer rikishiId, long net, long periodAdds, int percent) {}

	public record Summary(
			int days,
			LocalDate from,
			LocalDate to,
			Day today,
			Day yesterday,
			long totalVisitors,
			long totalPageViews,
			int mobilePercent,
			int maxPageViews,
			List<Day> daily,
			List<Ranked> topRikishi,
			List<Ranked> pageTypes,
			List<Ranked> referrers,
			List<Bookmarked> topBookmarked,
			LocalDate bookmarkSince // 즐겨찾기 집계를 시작한 날 (기록 없으면 null)
	) {}

	public Summary summary(int days) {
		return summary(days, LocalDate.now(VisitStatsCollector.ZONE));
	}

	Summary summary(int days, LocalDate today) {
		if (!PERIODS.contains(days)) {
			days = 30;
		}
		LocalDate from = today.minusDays(days - 1L);
		Date fromSql = Date.valueOf(from);

		Map<LocalDate, Day> byDate = jdbcTemplate.query(
						"SELECT stat_date, visitors, page_views, mobile_visitors FROM site_daily_stat WHERE stat_date >= ?",
						(rs, i) -> new Day(rs.getDate(1).toLocalDate(), rs.getInt(2), rs.getInt(3), rs.getInt(4)), fromSql)
				.stream().collect(Collectors.toMap(Day::date, Function.identity()));

		List<Day> daily = new ArrayList<>();
		for (LocalDate d = from; !d.isAfter(today); d = d.plusDays(1)) {
			daily.add(byDate.getOrDefault(d, new Day(d, 0, 0, 0)));
		}
		long visitors = daily.stream().mapToLong(Day::visitors).sum();
		long pageViews = daily.stream().mapToLong(Day::pageViews).sum();
		long mobile = daily.stream().mapToLong(Day::mobileVisitors).sum();
		int maxPv = daily.stream().mapToInt(Day::pageViews).max().orElse(0);

		return new Summary(days, from, today,
				daily.get(daily.size() - 1),
				daily.size() > 1 ? daily.get(daily.size() - 2) : new Day(today.minusDays(1), 0, 0, 0),
				visitors, pageViews, percent(mobile, visitors), maxPv, daily,
				topRikishi(fromSql), pageTypes(fromSql, pageViews), referrers(fromSql),
				topBookmarked(fromSql), bookmarkSince());
	}

	private List<Ranked> topRikishi(Date from) {
		List<long[]> rows = jdbcTemplate.query("""
						SELECT target_id, SUM(views) v FROM page_daily_stat
						WHERE page_type = 'RIKISHI' AND stat_date >= ? GROUP BY target_id ORDER BY v DESC LIMIT 10""",
				(rs, i) -> new long[]{rs.getLong(1), rs.getLong(2)}, from);
		long total = rows.stream().mapToLong(r -> r[1]).sum();
		Map<Integer, String> names = shikonaById(rows.stream().map(r -> (int) r[0]).toList());
		return rows.stream().map(r -> {
			int id = (int) r[0];
			return new Ranked(names.get(id), id, r[1], percent(r[1], total));
		}).toList();
	}

	/**
	 * 즐겨찾기 많은 리키시 Top 10 - 기간과 상관없이 집계 시작 후 누적(추가 − 해제) 순. 막대는 1위 대비.
	 * 브라우저 데이터를 지우면 해제가 오지 않아 실제보다 조금 많게 잡힐 수 있다.
	 */
	private List<Bookmarked> topBookmarked(Date from) {
		List<long[]> rows = jdbcTemplate.query("""
						SELECT rikishi_id, SUM(adds) - SUM(removes) net,
						  SUM(CASE WHEN stat_date >= ? THEN adds ELSE 0 END) recent
						FROM bookmark_daily_stat GROUP BY rikishi_id HAVING net > 0 ORDER BY net DESC, recent DESC LIMIT 10""",
				(rs, i) -> new long[]{rs.getLong(1), rs.getLong(2), rs.getLong(3)}, from);
		long max = rows.isEmpty() ? 0 : rows.get(0)[1];
		Map<Integer, String> names = shikonaById(rows.stream().map(r -> (int) r[0]).toList());
		return rows.stream().map(r -> {
			int id = (int) r[0];
			return new Bookmarked(names.get(id), id, r[1], r[2], percent(r[1], max));
		}).toList();
	}

	private LocalDate bookmarkSince() {
		Date d = jdbcTemplate.queryForObject("SELECT MIN(stat_date) FROM bookmark_daily_stat", Date.class);
		return d == null ? null : d.toLocalDate();
	}

	/** 통계 표에 쓰는 시코나 (한국어 → 한자 → #id) */
	private Map<Integer, String> shikonaById(List<Integer> ids) {
		Map<Integer, String> names = new HashMap<>();
		for (RikishiEntity e : rikishiRepository.findAllById(ids)) {
			names.put(e.getId(), e.getShikonaKr() != null ? e.getShikonaKr()
					: e.getShikonaJp() != null ? e.getShikonaJp() : "#" + e.getId());
		}
		ids.forEach(id -> names.putIfAbsent(id, "#" + id));
		return names;
	}

	private List<Ranked> pageTypes(Date from, long totalViews) {
		return jdbcTemplate.query("""
						SELECT page_type, SUM(views) v FROM page_daily_stat WHERE stat_date >= ?
						GROUP BY page_type ORDER BY v DESC""",
				(rs, i) -> {
					String type = rs.getString(1);
					String label;
					try {
						label = VisitClassifier.PageType.valueOf(type).getLabel();
					} catch (IllegalArgumentException e) {
						label = type;
					}
					return new Ranked(label, null, rs.getLong(2), percent(rs.getLong(2), totalViews));
				}, from);
	}

	private static final Map<String, String> REFERRER_LABEL = Map.of(
			"dcinside", "디시인사이드", "google", "구글", "naver", "네이버", "daum", "다음·카카오",
			"x", "X(트위터)", "facebook", "페이스북", "bing", "빙", "other", "기타");

	private List<Ranked> referrers(Date from) {
		List<Object[]> rows = jdbcTemplate.query("""
						SELECT source, SUM(visits) v FROM referrer_daily_stat WHERE stat_date >= ?
						GROUP BY source ORDER BY v DESC""",
				(rs, i) -> new Object[]{rs.getString(1), rs.getLong(2)}, from);
		long total = rows.stream().mapToLong(r -> (long) r[1]).sum();
		return rows.stream()
				.map(r -> new Ranked(REFERRER_LABEL.getOrDefault((String) r[0], (String) r[0]), null, (long) r[1], percent((long) r[1], total)))
				.toList();
	}

	private static int percent(long part, long whole) {
		return whole <= 0 ? 0 : (int) Math.round(part * 100.0 / whole);
	}
}
