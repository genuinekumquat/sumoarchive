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
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * 업데이트 소식(/updates). 이용자 눈에 보이는 변화만 날짜별로 한두 줄씩 보여준다.
 *
 * <p>내용은 {@code resources/updates.json}(최신 날짜가 위)에 두고, 기능을 커밋할 때 같이 한 줄 추가한다.
 * 항목마다 ko/ja를 쓰고, 한쪽 화면에만 해당하는 변화는 다른 쪽을 비워 두면 그 언어에선 숨긴다.
 * 앱 시작 때 한 번 읽는다 (파일이 깨져 있으면 앱이 뜨지 않음 — UpdatesControllerTest가 CI에서 먼저 잡는다).</p>
 *
 * <p>화면에선 월별로 접어 둔다. 가장 최근 달만 펼치고, 그 달 항목이 {@value #OPEN_PREVIOUS_BELOW}개 미만이면
 * 전달도 펼친다 (월초에 펼친 내용이 한두 줄뿐이지 않도록). 항목 수는 언어별로 보이는 것만 센다.</p>
 */
@Controller
public class UpdatesController {

	static final String RESOURCE = "updates.json";

	static final int OPEN_PREVIOUS_BELOW = 3;

	public record Item(String ko, String ja) {}

	public record Entry(LocalDate date, List<Item> items) {}

	/** 한 달치 소식. entries에는 그 언어로 보이는 항목만 남아 있다. */
	public record Month(YearMonth month, List<Entry> entries, int count, boolean open) {}

	private final List<Month> monthsKo;
	private final List<Month> monthsJa;

	public UpdatesController(ObjectMapper objectMapper) {
		List<Entry> entries = load(objectMapper);
		this.monthsKo = byMonth(entries, Item::ko);
		this.monthsJa = byMonth(entries, Item::ja);
	}

	@GetMapping("/updates")
	public String updates(Model model, Locale locale) {
		model.addAttribute("months", "ja".equals(locale.getLanguage()) ? monthsJa : monthsKo);
		return "updates";
	}

	static List<Entry> load(ObjectMapper objectMapper) {
		try (InputStream in = new ClassPathResource(RESOURCE).getInputStream()) {
			return List.copyOf(objectMapper.readValue(in, new TypeReference<List<Entry>>() {}));
		} catch (IOException e) {
			throw new UncheckedIOException(RESOURCE + "를 읽지 못했습니다", e);
		}
	}

	/** 날짜 내림차순 entries를 월별로 묶는다. 그 언어 문구가 없는 항목·날짜·달은 뺀다. */
	static List<Month> byMonth(List<Entry> entries, Function<Item, String> text) {
		Map<YearMonth, List<Entry>> grouped = new LinkedHashMap<>();
		for (Entry e : entries) {
			List<Item> visible = e.items().stream().filter(i -> text.apply(i) != null).toList();
			if (!visible.isEmpty()) {
				grouped.computeIfAbsent(YearMonth.from(e.date()), m -> new ArrayList<>()).add(new Entry(e.date(), visible));
			}
		}
		List<Month> months = new ArrayList<>();
		for (Map.Entry<YearMonth, List<Entry>> g : grouped.entrySet()) {
			int count = g.getValue().stream().mapToInt(e -> e.items().size()).sum();
			boolean open = months.isEmpty() || (months.size() == 1 && months.get(0).count() < OPEN_PREVIOUS_BELOW);
			months.add(new Month(g.getKey(), List.copyOf(g.getValue()), count, open));
		}
		return List.copyOf(months);
	}
}
