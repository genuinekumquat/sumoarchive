package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.domain.entity.FeedbackEntity;
import com.torikumilab.sumoarchive.domain.entity.constant.FeedbackCategory;
import com.torikumilab.sumoarchive.repository.FeedbackRepository;
import com.torikumilab.sumoarchive.service.exception.InvalidInputException;
import com.torikumilab.sumoarchive.service.security.RateLimiterService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

	@Mock
	private FeedbackRepository feedbackRepository;
	@Mock
	private RateLimiterService rateLimiterService;
	@InjectMocks
	private FeedbackService feedbackService;

	@Test
	@DisplayName("submit - 앞뒤 공백·줄바꿈 정리, 빈 연락처는 null, 언어와 보낸 페이지 저장")
	void submitNormalizes() {
		given(feedbackRepository.save(any(FeedbackEntity.class))).willAnswer(inv -> inv.getArgument(0));

		FeedbackEntity saved = feedbackService.submit("DATA", "  출신지가 틀려요\r\n확인 부탁해요  ", "   ",
				"/rikishi/12?lang=ja", Locale.JAPANESE, "1.1.1.1");

		assertThat(saved.getCategory()).isEqualTo(FeedbackCategory.DATA);
		assertThat(saved.getContent()).isEqualTo("출신지가 틀려요\n확인 부탁해요");
		assertThat(saved.getContact()).isNull();
		assertThat(saved.getPagePath()).isEqualTo("/rikishi/12?lang=ja");
		assertThat(saved.getLang()).isEqualTo("ja");
		assertThat(saved.isHandled()).isFalse();
	}

	@Test
	@DisplayName("submit - 내용이 비었거나 너무 길면 메시지 코드로 거절하고 저장하지 않음")
	void submitRejectsBadContent() {
		assertThatThrownBy(() -> feedbackService.submit("BUG", "   ", null, null, Locale.KOREAN, "1.1.1.1"))
				.isInstanceOf(InvalidInputException.class).hasMessage("feedback.error.content.required");
		assertThatThrownBy(() -> feedbackService.submit("BUG", "가".repeat(FeedbackService.CONTENT_MAX + 1), null, null, Locale.KOREAN, "1.1.1.1"))
				.isInstanceOf(InvalidInputException.class).hasMessage("feedback.error.content.length");
		assertThatThrownBy(() -> feedbackService.submit("BUG", "내용", "a".repeat(FeedbackService.CONTACT_MAX + 1), null, Locale.KOREAN, "1.1.1.1"))
				.isInstanceOf(InvalidInputException.class).hasMessage("feedback.error.contact.length");
		verify(feedbackRepository, never()).save(any());
		// 입력이 틀려 거절된 시도는 도배 제한을 쓰지 않음 (고쳐서 바로 다시 보낼 수 있게)
		verify(rateLimiterService, never()).checkFeedbackAllowed(any());
	}

	@Test
	@DisplayName("모르는 종류는 기타로, 보낸 페이지는 사이트 내부 경로만")
	void categoryAndPath() {
		assertThat(FeedbackService.parseCategory("SUGGESTION")).isEqualTo(FeedbackCategory.SUGGESTION);
		assertThat(FeedbackService.parseCategory("<script>")).isEqualTo(FeedbackCategory.OTHER);
		assertThat(FeedbackService.parseCategory(null)).isEqualTo(FeedbackCategory.OTHER);

		assertThat(FeedbackService.pagePath("/torikumi/5")).isEqualTo("/torikumi/5");
		assertThat(FeedbackService.pagePath("https://evil.example")).isNull();
		assertThat(FeedbackService.pagePath("//evil.example")).isNull();
		assertThat(FeedbackService.pagePath("javascript:alert(1)")).isNull();
		assertThat(FeedbackService.pagePath("/feedback?from=/x")).isNull();
		assertThat(FeedbackService.pagePath("/" + "a".repeat(300))).hasSize(255);
	}
}
