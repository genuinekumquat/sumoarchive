package com.torikumilab.sumoarchive.service.stats;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 방문 통계용 판별: 봇인지, 어떤 페이지인지, 어디서 왔는지. 상태가 없는 순수 함수만 둔다(VisitClassifierTest).
 */
public final class VisitClassifier {

	private VisitClassifier() {
	}

	/** 공개 페이지 종류. DB page_daily_stat.page_type에 이름 그대로 저장한다. */
	public enum PageType {
		MAIN("메인"), RIKISHI("리키시 프로필"), TORIKUMI("경기 상세"), SEARCH("검색"),
		BOOKMARK("즐겨찾기"), UPDATES("업데이트 소식"), PRIVACY("개인정보 안내");

		private final String label;

		PageType(String label) {
			this.label = label;
		}

		public String getLabel() {
			return label;
		}
	}

	/** 어떤 페이지인지 + 대상 id(리키시·경기, 없으면 0) */
	public record Page(PageType type, int targetId) {}

	// 2026.10.12 Nginx 로그 집계와 같은 기준 + 링크 미리보기·모니터링 도구
	// + 2026.10.27 로그에서 사람으로 세던 것: meta-externalagent(Meta AI 수집기), DCInsideGallery-ChkUrl, Yeti(네이버)
	private static final Pattern BOT_UA = Pattern.compile(
			"bot|crawl|spider|slurp|curl|wget|python|go-http|headless|playwright|preview|scan|monitor|"
					+ "facebookexternalhit|externalagent|chkurl|yeti|java/|okhttp|axios|node-fetch|httpclient|libwww|lighthouse");

	private static final Pattern RIKISHI = Pattern.compile("^/rikishi/(\\d+)$");
	private static final Pattern TORIKUMI = Pattern.compile("^/torikumi/(\\d+)$");
	private static final Pattern IP_HOST = Pattern.compile("^[0-9.:\\[\\]]+$");

	/**
	 * 봇·도구로 보이면 true. 브라우저는 항상 Accept-Language를 보내므로 없으면 봇으로 본다
	 * (Nginx 로그 집계에서 새벽에도 시간당 10명꼴로 남던 것들).
	 */
	public static boolean isBot(String userAgent, String acceptLanguage) {
		if (userAgent == null || userAgent.isBlank() || acceptLanguage == null || acceptLanguage.isBlank()) {
			return true;
		}
		return BOT_UA.matcher(userAgent.toLowerCase(Locale.ROOT)).find();
	}

	public static boolean isMobile(String userAgent) {
		return userAgent != null && (userAgent.contains("Mobile") || userAgent.contains("Android") || userAgent.contains("iPhone"));
	}

	/** 요청 경로 → 페이지. 통계 대상이 아니면 null. */
	public static Page classify(String path) {
		if (path == null) {
			return null;
		}
		switch (path) {
			case "/":
				return new Page(PageType.MAIN, 0);
			case "/search":
				return new Page(PageType.SEARCH, 0);
			case "/bookmark":
				return new Page(PageType.BOOKMARK, 0);
			case "/updates":
				return new Page(PageType.UPDATES, 0);
			case "/privacy":
				return new Page(PageType.PRIVACY, 0);
			default:
				break;
		}
		Matcher m = RIKISHI.matcher(path);
		if (m.matches()) {
			return idPage(PageType.RIKISHI, m.group(1));
		}
		m = TORIKUMI.matcher(path);
		if (m.matches()) {
			return idPage(PageType.TORIKUMI, m.group(1));
		}
		return null;
	}

	private static Page idPage(PageType type, String id) {
		try {
			return new Page(type, Integer.parseInt(id));
		} catch (NumberFormatException e) {
			return null; // int 범위를 넘는 id - 실제 페이지는 404라 여기까지 오지 않음
		}
	}

	/**
	 * Referer → 유입처 묶음(dcinside·google·naver·daum·x·facebook·bing·other). 사이트 안 이동, 없음, IP 주소로 온 것(스캐너)은 null.
	 */
	public static String referrerSource(String referer, String ownHost) {
		if (referer == null || referer.isBlank()) {
			return null;
		}
		String host;
		try {
			host = URI.create(referer.strip()).getHost();
		} catch (IllegalArgumentException e) {
			return null;
		}
		if (host == null || IP_HOST.matcher(host).matches()) {
			return null;
		}
		host = host.toLowerCase(Locale.ROOT);
		if (host.equals("sumoarchive.com") || host.endsWith(".sumoarchive.com") || host.equalsIgnoreCase(ownHost)) {
			return null;
		}
		if (host.endsWith("dcinside.com")) return "dcinside";
		if (host.contains("google.")) return "google";
		if (host.endsWith("naver.com")) return "naver";
		if (host.endsWith("daum.net") || host.endsWith("kakao.com")) return "daum";
		if (host.equals("x.com") || host.endsWith(".x.com") || host.equals("t.co") || host.endsWith("twitter.com")) return "x";
		if (host.endsWith("facebook.com")) return "facebook";
		if (host.endsWith("bing.com")) return "bing";
		return "other";
	}
}
