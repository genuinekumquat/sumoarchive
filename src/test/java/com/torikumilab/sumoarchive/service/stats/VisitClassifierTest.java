package com.torikumilab.sumoarchive.service.stats;

import com.torikumilab.sumoarchive.service.stats.VisitClassifier.Page;
import com.torikumilab.sumoarchive.service.stats.VisitClassifier.PageType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VisitClassifierTest {

	private static final String CHROME_ANDROID = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/30.0 Chrome/143.0.0.0 Mobile Safari/537.36";
	private static final String CHROME_PC = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/153.0.0.0 Safari/537.36";

	@Test
	@DisplayName("봇 판별 - 검색엔진·도구·언어 헤더 없는 요청은 봇")
	void isBot() {
		assertThat(VisitClassifier.isBot(CHROME_ANDROID, "ko-KR,ko;q=0.9")).isFalse();
		assertThat(VisitClassifier.isBot(CHROME_PC, "ja")).isFalse();
		assertThat(VisitClassifier.isBot("Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)", "en")).isTrue();
		assertThat(VisitClassifier.isBot("Mozilla/5.0 (compatible; Yeti/1.1; +https://naver.me/spd)", null)).isTrue(); // 네이버 수집기: 언어 헤더 없음
		assertThat(VisitClassifier.isBot("curl/8.4.0", "ko")).isTrue();
		assertThat(VisitClassifier.isBot("python-requests/2.31", "ko")).isTrue();
		assertThat(VisitClassifier.isBot(CHROME_PC, null)).isTrue();
		assertThat(VisitClassifier.isBot(null, "ko")).isTrue();
	}

	@Test
	@DisplayName("페이지 종류 - 공개 HTML 페이지만, 경기 패널 조각은 제외")
	void classify() {
		assertThat(VisitClassifier.classify("/")).isEqualTo(new Page(PageType.MAIN, 0));
		assertThat(VisitClassifier.classify("/rikishi/1257")).isEqualTo(new Page(PageType.RIKISHI, 1257));
		assertThat(VisitClassifier.classify("/torikumi/42")).isEqualTo(new Page(PageType.TORIKUMI, 42));
		assertThat(VisitClassifier.classify("/search")).isEqualTo(new Page(PageType.SEARCH, 0));
		assertThat(VisitClassifier.classify("/updates")).isEqualTo(new Page(PageType.UPDATES, 0));
		assertThat(VisitClassifier.classify("/torikumi/42/fragment")).isNull();
		assertThat(VisitClassifier.classify("/rikishi/abc")).isNull();
		assertThat(VisitClassifier.classify("/rikishi/99999999999")).isNull();
		assertThat(VisitClassifier.classify("/admin/stats")).isNull();
	}

	@Test
	@DisplayName("유입처 - 사이트 단위로 묶고, 사이트 안 이동·IP 주소·없음은 제외")
	void referrerSource() {
		assertThat(VisitClassifier.referrerSource("https://m.dcinside.com/board/sumo/12345", "sumoarchive.com")).isEqualTo("dcinside");
		assertThat(VisitClassifier.referrerSource("https://gall.dcinside.com/mgallery/board/view/?id=sumo", "sumoarchive.com")).isEqualTo("dcinside");
		assertThat(VisitClassifier.referrerSource("https://www.google.co.kr/", "sumoarchive.com")).isEqualTo("google");
		assertThat(VisitClassifier.referrerSource("https://m.search.naver.com/search.naver?query=x", "sumoarchive.com")).isEqualTo("naver");
		assertThat(VisitClassifier.referrerSource("https://t.co/abc", "sumoarchive.com")).isEqualTo("x");
		assertThat(VisitClassifier.referrerSource("https://example.org/post", "sumoarchive.com")).isEqualTo("other");
		assertThat(VisitClassifier.referrerSource("https://sumoarchive.com/rikishi/1", "sumoarchive.com")).isNull();
		assertThat(VisitClassifier.referrerSource("http://localhost:8081/", "localhost")).isNull();
		assertThat(VisitClassifier.referrerSource("http://54.180.53.214/", "sumoarchive.com")).isNull();
		assertThat(VisitClassifier.referrerSource("not a url %%", "sumoarchive.com")).isNull();
		assertThat(VisitClassifier.referrerSource(null, "sumoarchive.com")).isNull();
	}

	@Test
	@DisplayName("모바일 판별")
	void isMobile() {
		assertThat(VisitClassifier.isMobile(CHROME_ANDROID)).isTrue();
		assertThat(VisitClassifier.isMobile(CHROME_PC)).isFalse();
	}
}
