package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.domain.entity.FeedbackEntity;
import com.torikumilab.sumoarchive.domain.entity.constant.FeedbackCategory;
import com.torikumilab.sumoarchive.repository.FeedbackRepository;
import com.torikumilab.sumoarchive.service.exception.InvalidInputException;
import com.torikumilab.sumoarchive.service.security.RateLimiterService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * 방문자 의견·오류 보고. 공개 폼(/feedback)이 쓰고, 관리자 「의견함」(/admin/feedback)이 읽는다.
 * 입력 오류 메시지는 메시지 코드(feedback.error.*)라 화면 언어로 바뀐다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedbackService {

	public static final int CONTENT_MAX = 1000;
	public static final int CONTACT_MAX = 100;
	static final int PATH_MAX = 255;
	static final int PAGE_SIZE = 30;

	private final FeedbackRepository feedbackRepository;
	private final RateLimiterService rateLimiterService;

	/**
	 * 입력을 먼저 검사하고 나서 도배 제한을 센다 — 내용을 비워 거절된 시도가 30초 제한을 써 버리면
	 * 고쳐서 바로 다시 보낼 때 막히기 때문.
	 */
	@Transactional
	public FeedbackEntity submit(String category, String content, String contact, String from, Locale locale,
								 String clientIp) {
		String body = content == null ? "" : content.strip().replace("\r\n", "\n");
		if (body.isEmpty()) {
			throw new InvalidInputException("feedback.error.content.required");
		}
		if (body.length() > CONTENT_MAX) {
			throw new InvalidInputException("feedback.error.content.length", CONTENT_MAX);
		}
		String contactValue = contact == null || contact.isBlank() ? null : contact.strip();
		if (contactValue != null && contactValue.length() > CONTACT_MAX) {
			throw new InvalidInputException("feedback.error.contact.length", CONTACT_MAX);
		}
		rateLimiterService.checkFeedbackAllowed(clientIp);
		return feedbackRepository.save(FeedbackEntity.builder()
				.category(parseCategory(category))
				.content(body)
				.contact(contactValue)
				.pagePath(pagePath(from))
				.lang(locale.getLanguage())
				.build());
	}

	/** 모르는 값이면 기타로 받는다 (종류를 잘못 골랐다고 의견을 버리지 않게). */
	public static FeedbackCategory parseCategory(String value) {
		if (value != null) {
			for (FeedbackCategory c : FeedbackCategory.values()) {
				if (c.name().equals(value)) {
					return c;
				}
			}
		}
		return FeedbackCategory.OTHER;
	}

	/**
	 * 보낸 페이지 경로. 푸터 링크가 넘겨주는 값이지만 주소창에서 바꿀 수 있으므로
	 * 사이트 내부 경로(/로 시작, //는 제외)만 받고, 나머지는 버린다(의견 자체는 받음).
	 */
	public static String pagePath(String from) {
		if (from == null) {
			return null;
		}
		String v = from.strip();
		if (!v.startsWith("/") || v.startsWith("//") || v.startsWith("/feedback")) {
			return null;
		}
		return v.length() > PATH_MAX ? v.substring(0, PATH_MAX) : v;
	}

	public Page<FeedbackEntity> list(boolean unhandledOnly, int page) {
		PageRequest pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE);
		return unhandledOnly
				? feedbackRepository.findByHandledFalseOrderByIdDesc(pageable)
				: feedbackRepository.findAllByOrderByIdDesc(pageable);
	}

	public long countUnhandled() {
		return feedbackRepository.countByHandledFalse();
	}

	@Transactional
	public void setHandled(Integer id, boolean handled) {
		get(id).setHandled(handled);
	}

	@Transactional
	public void delete(Integer id) {
		feedbackRepository.delete(get(id));
	}

	private FeedbackEntity get(Integer id) {
		return feedbackRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("의견을 찾을 수 없습니다: " + id));
	}
}
