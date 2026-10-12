package com.torikumilab.sumoarchive.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;

/**
 * 업데이트 소식(/updates). 이용자 눈에 보이는 변화만 날짜별로 한두 줄씩 보여준다.
 *
 * <p>내용은 {@code resources/updates.json}(최신 날짜가 위)에 두고, 기능을 커밋할 때 같이 한 줄 추가한다.
 * 항목마다 ko/ja를 쓰고, 한쪽 화면에만 해당하는 변화는 다른 쪽을 비워 두면 그 언어에선 숨긴다.
 * 앱 시작 때 한 번 읽는다 (파일이 깨져 있으면 앱이 뜨지 않음 — UpdatesControllerTest가 CI에서 먼저 잡는다).</p>
 */
@Controller
public class UpdatesController {

	static final String RESOURCE = "updates.json";

	public record Item(String ko, String ja) {}

	public record Entry(LocalDate date, List<Item> items) {}

	private final List<Entry> entries;

	public UpdatesController(ObjectMapper objectMapper) {
		this.entries = load(objectMapper);
	}

	@GetMapping("/updates")
	public String updates(Model model) {
		model.addAttribute("entries", entries);
		return "updates";
	}

	static List<Entry> load(ObjectMapper objectMapper) {
		try (InputStream in = new ClassPathResource(RESOURCE).getInputStream()) {
			return List.copyOf(objectMapper.readValue(in, new TypeReference<List<Entry>>() {}));
		} catch (IOException e) {
			throw new UncheckedIOException(RESOURCE + "를 읽지 못했습니다", e);
		}
	}
}
