package com.torikumilab.sumoarchive.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 토리쿠미에 저장된 유튜브 URL에서 영상 ID를 뽑아 임베드용 주소로 바꿔준다.
 * watch?v=, youtu.be/, /embed/, /shorts/ 형태를 지원하고, 못 알아보면 null을 반환한다
 * (템플릿은 그 경우 임베드 대신 원본 링크만 노출).
 */
public final class YoutubeUrlUtil {

	private YoutubeUrlUtil() {
	}

	private static final Pattern VIDEO_ID = Pattern.compile(
			"(?:youtube\\.com/(?:watch\\?(?:.*&)?v=|embed/|shorts/|live/)|youtu\\.be/)([A-Za-z0-9_-]{11})"
	);

	public static String extractVideoId(String url) {
		if (url == null || url.isBlank()) {
			return null;
		}
		Matcher m = VIDEO_ID.matcher(url);
		return m.find() ? m.group(1) : null;
	}

	public static String toEmbedUrl(String url) {
		String id = extractVideoId(url);
		return id == null ? null : "https://www.youtube.com/embed/" + id;
	}
}
