package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.domain.entity.BanzukeEntity;
import com.torikumilab.sumoarchive.domain.entity.BashoEntity;
import com.torikumilab.sumoarchive.domain.entity.RikishiEntity;
import com.torikumilab.sumoarchive.domain.entity.constant.BashoMonth;
import com.torikumilab.sumoarchive.domain.entity.constant.RankName;
import com.torikumilab.sumoarchive.repository.BanzukeRepository;
import com.torikumilab.sumoarchive.repository.BashoRepository;
import com.torikumilab.sumoarchive.util.RankDisplayUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 프로필·검색·즐겨찾기에 보여줄 최고위.
 *
 * <p>관리자가 직접 입력한 {@code highest_rank}가 있으면 그 값, 없으면 적재된 반즈케 중 가장 높은 지위
 * (같은 지위면 처음 오른 바쇼)로 계산한다. 반즈케는 2019년부터만 있어서, 그 전에 데뷔한 리키시의 계산값은
 * 실제 최고위보다 낮을 수 있다 → {@code partial}로 표시한다. 단 sumo-api 전체 계급 이력과 대조해 같다고 확인된
 * 리키시({@code highest_rank_verified})와 요코즈나(더 높은 지위가 없음)는 빼고, 실제가 더 높았던 리키시는
 * {@code highest_rank}에 직접 입력해 둔다.</p>
 */
@Service
@RequiredArgsConstructor
public class HighestRankService {

	/** 높은 지위가 먼저. 번호 없는 지위(요코즈나~코무스비)는 번호를 보지 않고, 같으면 먼저 오른 바쇼. */
	static final Comparator<BanzukeEntity> HIGHEST_FIRST = Comparator
			.comparing((BanzukeEntity b) -> b.getRankName().ordinal())
			.thenComparing(b -> isNumbered(b.getRankName()) ? b.getRankValue() : null,
					Comparator.nullsLast(Comparator.naturalOrder()))
			.thenComparing(b -> b.getBashoEntity().getStartDate());

	private final BanzukeRepository banzukeRepository;
	private final BashoRepository bashoRepository;

	/**
	 * @param rank      화면 표기 ("마에가시라3" / "前頭3枚目", 직접 입력값은 "오제키" / "大関")
	 * @param since     처음 오른 바쇼 ("2024년 5월 나츠바쇼" / "2024年05月場所"). 직접 입력값은 모를 때 null ({@link #manualSince})
	 * @param partial   2019년 전 데뷔(또는 데뷔일 모름)라 적재된 기록만으로 계산했고, 실제와 같은지 확인되지 않은 값
	 */
	public record HighestRank(String rank, String rankJp, String since, String sinceJp, boolean partial) {
	}

	/**
	 * 계산에 필요한 리키시 정보. 검색 결과처럼 데뷔 연도만 있으면 그해 1월로 둔다
	 * (기록 시작이 1월 바쇼라 연도만으로도 판단이 같다). 검색·즐겨찾기 카드는 partial을 쓰지 않아 verified를 몰라도 된다.
	 */
	public record Subject(Integer rikishiId, String manualHighestRank, String manualSince, YearMonth debut, boolean verified) {
		public Subject(Integer rikishiId, String manualHighestRank, YearMonth debut, boolean verified) {
			this(rikishiId, manualHighestRank, null, debut, verified);
		}

		public static Subject of(RikishiEntity r) {
			return new Subject(r.getId(), r.getHighestRank(), r.getHighestRankSince(),
					r.getDebutDate() != null ? YearMonth.from(r.getDebutDate()) : null, r.isHighestRankVerified());
		}
	}

	/** 프로필용: 이미 불러온 그 리키시의 반즈케 전체(바쇼 fetch)로 계산. */
	public HighestRank resolve(RikishiEntity r, List<BanzukeEntity> banzukeHistory) {
		return resolve(Subject.of(r), banzukeHistory, dataStart());
	}

	/** 검색·즐겨찾기용: 여러 리키시를 한 번에 (반즈케 1쿼리). 결과가 없는 리키시는 맵에 없다. */
	public Map<Integer, HighestRank> resolveAll(Collection<Subject> subjects) {
		if (subjects.isEmpty()) {
			return Map.of();
		}
		Map<Integer, List<BanzukeEntity>> byRikishi = banzukeRepository
				.findByRikishiIdsFetchBasho(subjects.stream().map(Subject::rikishiId).toList())
				.stream()
				.collect(Collectors.groupingBy(b -> b.getRikishiEntity().getId()));
		LocalDate dataStart = dataStart();
		Map<Integer, HighestRank> result = new HashMap<>();
		for (Subject s : subjects) {
			HighestRank h = resolve(s, byRikishi.getOrDefault(s.rikishiId(), List.of()), dataStart);
			if (h != null) {
				result.put(s.rikishiId(), h);
			}
		}
		return result;
	}

	/**
	 * 관리자 화면용 점검 결과. computed는 직접 입력값을 빼고 반즈케로만 계산한 값(기록이 없으면 null),
	 * outdated는 직접 입력값이 그보다 낮다는 뜻 - 입력한 뒤 더 올라갔으니(예: 「마에가시라」로 넣은 뒤 코무스비 승진)
	 * 지우거나 고쳐야 한다.
	 */
	public record ManualCheck(HighestRank computed, boolean outdated) {
	}

	/** 관리자 목록·수정 화면용 (반즈케 1쿼리). 결과가 없는 리키시는 맵에 없다. */
	public Map<Integer, ManualCheck> checkAll(Collection<RikishiEntity> rikishi) {
		if (rikishi.isEmpty()) {
			return Map.of();
		}
		Map<Integer, List<BanzukeEntity>> byRikishi = banzukeRepository
				.findByRikishiIdsFetchBasho(rikishi.stream().map(RikishiEntity::getId).toList())
				.stream()
				.collect(Collectors.groupingBy(b -> b.getRikishiEntity().getId()));
		LocalDate dataStart = dataStart();
		Map<Integer, ManualCheck> result = new HashMap<>();
		for (RikishiEntity r : rikishi) {
			List<BanzukeEntity> history = byRikishi.getOrDefault(r.getId(), List.of());
			Subject s = Subject.of(r);
			HighestRank computed = resolve(new Subject(s.rikishiId(), null, s.debut(), s.verified()), history, dataStart);
			BanzukeEntity best = history.stream().min(HIGHEST_FIRST).orElse(null);
			result.put(r.getId(), new ManualCheck(computed, best != null && isLowerThan(r.getHighestRank(), best)));
		}
		return result;
	}

	/**
	 * 직접 입력값("Komusubi", "Maegashira 4")이 반즈케 최고 기록보다 낮은지. 번호가 붙는 지위(마에가시라~)인데
	 * 번호가 없는 옛 입력값은 같은 지위여도 낮은 것으로 본다 - 계산값이 번호까지 정확하다.
	 * 해석할 수 없는 값(한글 등)이나 빈 값은 false.
	 */
	static boolean isLowerThan(String manual, BanzukeEntity best) {
		if (manual == null || manual.isBlank()) {
			return false;
		}
		String[] parts = manual.strip().split("\\s+");
		RankName rankName;
		try {
			rankName = RankName.valueOf(parts[0]);
		} catch (IllegalArgumentException e) {
			return false;
		}
		int cmp = Integer.compare(rankName.ordinal(), best.getRankName().ordinal());
		if (cmp != 0 || !isNumbered(rankName)) {
			return cmp > 0;
		}
		if (parts.length < 2 || !parts[1].matches("\\d{1,3}")) {
			return true;
		}
		return best.getRankValue() != null && Integer.parseInt(parts[1]) > best.getRankValue();
	}

	static HighestRank resolve(Subject s, List<BanzukeEntity> banzukeHistory, LocalDate dataStart) {
		String manual = s.manualHighestRank();
		if (manual != null && !manual.isBlank()) {
			YearMonth since = manualSince(s, manual, banzukeHistory, dataStart);
			return new HighestRank(RankDisplayUtil.toKorean(manual), RankDisplayUtil.toJapanese(manual),
					since != null ? sinceKr(since) : null, since != null ? sinceJp(since) : null, false);
		}
		BanzukeEntity best = banzukeHistory.stream().min(HIGHEST_FIRST).orElse(null);
		if (best == null) {
			return null;
		}
		boolean partial = !s.verified() && best.getRankName() != RankName.Yokozuna
				&& (dataStart == null || s.debut() == null || s.debut().isBefore(YearMonth.from(dataStart)));
		YearMonth since = bashoMonth(best.getBashoEntity());
		return new HighestRank(
				RankDisplayUtil.rankDisplayKorean(best.getRankName(), best.getRankValue()),
				RankDisplayUtil.rankDisplay(best.getRankName(), best.getRankValue()),
				sinceKr(since), sinceJp(since), partial);
	}

	/**
	 * 직접 입력한 최고위에 처음 오른 바쇼. 저장된 값(sumo-api 전체 이력, V19)이 우선이고, 없으면 반즈케에서 그 지위
	 * (번호가 있으면 번호까지)에 처음 오른 바쇼 - 단 기록 시작(2019년) 뒤에 데뷔한 리키시만. 그 전 데뷔면 2019년 전에
	 * 이미 올랐을 수 있어(예: 타카야스 오제키 2017년) 반즈케의 첫 기록이 틀린 바쇼일 수 있다 → null.
	 */
	private static YearMonth manualSince(Subject s, String manual, List<BanzukeEntity> banzukeHistory, LocalDate dataStart) {
		String stored = s.manualSince();
		if (stored != null && stored.matches("\\d{6}")) {
			return YearMonth.of(Integer.parseInt(stored.substring(0, 4)), Integer.parseInt(stored.substring(4)));
		}
		if (dataStart == null || s.debut() == null || s.debut().isBefore(YearMonth.from(dataStart))) {
			return null;
		}
		String[] parts = manual.strip().split("\\s+");
		return banzukeHistory.stream()
				.filter(b -> b.getRankName().name().equals(parts[0]))
				.filter(b -> parts.length < 2 || String.valueOf(b.getRankValue()).equals(parts[1]))
				.map(b -> bashoMonth(b.getBashoEntity()))
				.min(Comparator.naturalOrder())
				.orElse(null);
	}

	private static YearMonth bashoMonth(BashoEntity basho) {
		return YearMonth.of(basho.getBashoYear(), basho.getBashoMonth().getMonthValue());
	}

	private static String sinceKr(YearMonth ym) {
		return ym.getYear() + "년 " + ym.getMonthValue() + "월 " + toBashoMonth(ym).getDisplayNameKr();
	}

	private static String sinceJp(YearMonth ym) {
		return ym.getYear() + "年" + String.format("%02d", ym.getMonthValue()) + "月場所";
	}

	private static BashoMonth toBashoMonth(YearMonth ym) {
		return Arrays.stream(BashoMonth.values())
				.filter(m -> m.getMonthValue() == ym.getMonthValue())
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("바쇼 달이 아님: " + ym));
	}

	/** 반즈케가 적재된 가장 이른 바쇼의 시작일 (2019년 1월). 반즈케가 하나도 없으면 null. */
	private LocalDate dataStart() {
		return bashoRepository.findEarliestStartDateWithBanzuke();
	}

	private static boolean isNumbered(RankName rankName) {
		return rankName.ordinal() >= RankName.Maegashira.ordinal();
	}
}
