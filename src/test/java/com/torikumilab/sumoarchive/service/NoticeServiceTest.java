package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.domain.dto.NoticeDTO;
import com.torikumilab.sumoarchive.domain.entity.NoticeEntity;
import com.torikumilab.sumoarchive.repository.NoticeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class NoticeServiceTest {

	@Mock
	private NoticeRepository noticeRepository;
	@InjectMocks
	private NoticeService noticeService;

	private static NoticeEntity notice(int id, String titleJa, String bodyJa, boolean active) {
		NoticeEntity n = NoticeEntity.builder()
				.titleKo("제목").bodyKo("본문").titleJa(titleJa).bodyJa(bodyJa).active(active).build();
		ReflectionTestUtils.setField(n, "id", id);
		return n;
	}

	@Test
	@DisplayName("findActive - 요청 언어 문구로 내려주고, 일본어 문구가 없으면 일본어 화면에는 띄우지 않음")
	void findActiveByLocale() {
		given(noticeRepository.findFirstByActiveTrueOrderByIdDesc())
				.willReturn(Optional.of(notice(3, "タイトル", "本文", true)));
		assertThat(noticeService.findActive(Locale.KOREAN))
				.contains(new NoticeDTO(3, "제목", "본문", null));
		assertThat(noticeService.findActive(Locale.JAPANESE))
				.contains(new NoticeDTO(3, "タイトル", "本文", null));

		given(noticeRepository.findFirstByActiveTrueOrderByIdDesc())
				.willReturn(Optional.of(notice(4, null, null, true)));
		assertThat(noticeService.findActive(Locale.KOREAN)).isPresent();
		assertThat(noticeService.findActive(Locale.JAPANESE)).isEmpty();
	}

	@Test
	@DisplayName("activate - 고른 공지만 켜지고 나머지는 꺼짐")
	void activateKeepsOnlyOne() {
		NoticeEntity a = notice(1, null, null, true);
		NoticeEntity b = notice(2, null, null, false);
		given(noticeRepository.findById(2)).willReturn(Optional.of(b));
		given(noticeRepository.findAll()).willReturn(List.of(a, b));

		noticeService.activate(2);

		assertThat(a.isActive()).isFalse();
		assertThat(b.isActive()).isTrue();
	}

	@Test
	@DisplayName("link - 사이트 내부 경로와 https만 허용, 빈 값은 null")
	void linkValidation() {
		assertThat(NoticeService.link(" /updates ")).isEqualTo("/updates");
		assertThat(NoticeService.link("https://example.com")).isEqualTo("https://example.com");
		assertThat(NoticeService.link("  ")).isNull();
		assertThatThrownBy(() -> NoticeService.link("javascript:alert(1)")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> NoticeService.link("//evil.example")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> NoticeService.link("http://example.com")).isInstanceOf(IllegalArgumentException.class);
	}
}
