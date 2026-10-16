package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.domain.dto.NoticeDTO;
import com.torikumilab.sumoarchive.domain.entity.NoticeEntity;
import com.torikumilab.sumoarchive.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 메인 팝업 공지. 한 번에 하나만 켜져 있게 한다(켜면 나머지는 꺼짐) — 관리자 화면에서 "지금 뜨는 공지"가 헷갈리지 않게.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

	static final int TITLE_MAX = 100;
	static final int BODY_MAX = 1000;
	static final int LINK_MAX = 255;

	private final NoticeRepository noticeRepository;

	/** 메인 팝업용 - 켜진 공지를 요청 언어로. 일본어 문구가 없으면 일본어 화면에서는 띄우지 않는다. */
	public Optional<NoticeDTO> findActive(Locale locale) {
		return noticeRepository.findFirstByActiveTrueOrderByIdDesc().flatMap(n -> {
			if ("ja".equals(locale.getLanguage())) {
				if (isBlank(n.getTitleJa()) || isBlank(n.getBodyJa())) {
					return Optional.empty();
				}
				return Optional.of(new NoticeDTO(n.getId(), n.getTitleJa(), n.getBodyJa(), n.getLinkUrl()));
			}
			return Optional.of(new NoticeDTO(n.getId(), n.getTitleKo(), n.getBodyKo(), n.getLinkUrl()));
		});
	}

	public List<NoticeEntity> list() {
		return noticeRepository.findAllByOrderByIdDesc();
	}

	/** 새 공지. activate면 바로 켜고 나머지는 끈다. */
	@Transactional
	public NoticeEntity create(String titleKo, String bodyKo, String titleJa, String bodyJa, String linkUrl,
							   boolean activate) {
		NoticeEntity notice = noticeRepository.save(NoticeEntity.builder()
				.titleKo(required(titleKo, "한국어 제목", TITLE_MAX))
				.bodyKo(required(bodyKo, "한국어 본문", BODY_MAX))
				.titleJa(optional(titleJa, "일본어 제목", TITLE_MAX))
				.bodyJa(optional(bodyJa, "일본어 본문", BODY_MAX))
				.linkUrl(link(linkUrl))
				.active(false)
				.build());
		if (activate) {
			activate(notice.getId());
		}
		return notice;
	}

	@Transactional
	public void update(Integer id, String titleKo, String bodyKo, String titleJa, String bodyJa, String linkUrl) {
		get(id).update(required(titleKo, "한국어 제목", TITLE_MAX), required(bodyKo, "한국어 본문", BODY_MAX),
				optional(titleJa, "일본어 제목", TITLE_MAX), optional(bodyJa, "일본어 본문", BODY_MAX), link(linkUrl));
	}

	@Transactional
	public void activate(Integer id) {
		NoticeEntity target = get(id);
		noticeRepository.findAll().forEach(n -> n.setActive(n == target));
	}

	@Transactional
	public void deactivate(Integer id) {
		get(id).setActive(false);
	}

	@Transactional
	public void delete(Integer id) {
		noticeRepository.delete(get(id));
	}

	private NoticeEntity get(Integer id) {
		return noticeRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("공지를 찾을 수 없습니다: " + id));
	}

	private static String required(String value, String label, int max) {
		String v = optional(value, label, max);
		if (v == null) {
			throw new IllegalArgumentException(label + "을(를) 입력하세요.");
		}
		return v;
	}

	/** 앞뒤 공백 제거, 비었으면 null. 줄바꿈은 \r\n을 \n으로 통일한다. */
	private static String optional(String value, String label, int max) {
		if (isBlank(value)) {
			return null;
		}
		String v = value.strip().replace("\r\n", "\n");
		if (v.length() > max) {
			throw new IllegalArgumentException(label + "은(는) " + max + "자까지입니다.");
		}
		return v;
	}

	/** 사이트 내부 경로(/로 시작, //는 제외) 또는 https:// 주소만. javascript: 같은 건 막는다. */
	static String link(String value) {
		String v = optional(value, "링크", LINK_MAX);
		if (v == null) {
			return null;
		}
		boolean internal = v.startsWith("/") && !v.startsWith("//");
		if (!internal && !v.startsWith("https://")) {
			throw new IllegalArgumentException("링크는 /로 시작하는 사이트 내부 경로나 https:// 주소만 쓸 수 있습니다.");
		}
		return v;
	}

	private static boolean isBlank(String s) {
		return s == null || s.isBlank();
	}
}
