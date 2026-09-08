package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.domain.dto.RikishiOptionDTO;
import com.torikumilab.sumoarchive.domain.dto.TorikumiBoardDTO;
import com.torikumilab.sumoarchive.domain.dto.TorikumiFormDTO;
import com.torikumilab.sumoarchive.domain.dto.TorikumiRowDTO;
import com.torikumilab.sumoarchive.domain.entity.BashoEntity;
import com.torikumilab.sumoarchive.domain.entity.RikishiEntity;
import com.torikumilab.sumoarchive.domain.entity.TorikumiEntity;
import com.torikumilab.sumoarchive.domain.entity.constant.Division;
import com.torikumilab.sumoarchive.domain.entity.constant.ResultType;
import com.torikumilab.sumoarchive.domain.entity.constant.Side;
import com.torikumilab.sumoarchive.repository.BashoRepository;
import com.torikumilab.sumoarchive.repository.RikishiRepository;
import com.torikumilab.sumoarchive.repository.TorikumiRepository;
import com.torikumilab.sumoarchive.util.KimariteDisplayUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 관리자 "데이터 수동 갱신" 중 토리쿠미(取組, 대전) 관리. 바쇼별로 15일치 대전 결과를
 * 관리자가 직접 추가/수정/삭제한다. /admin/** 경로라 AdminAuthInterceptor가 세션 isAdmin을
 * 먼저 검사한다.
 *
 * <p>입력한 대전은 별도 동기화 없이 토리쿠미 상세 페이지 / 리키시 호시토리표 / 키마리테 통계에
 * 그대로 반영된다.</p>
 */
@Service
@RequiredArgsConstructor
public class TorikumiAdminService {

	private final TorikumiRepository torikumiRepository;
	private final BashoRepository bashoRepository;
	private final RikishiRepository rikishiRepository;

	@Transactional(readOnly = true)
	public TorikumiBoardDTO getBoard(Integer bashoId, int day, Division division) {
		BashoEntity basho = findBasho(bashoId);
		List<TorikumiRowDTO> rows = torikumiRepository.findForAdmin(basho.getId(), day, division)
				.stream()
				.map(TorikumiAdminService::toRow)
				.toList();
		return new TorikumiBoardDTO(basho.getId(), bashoLabel(basho), day, division, rows);
	}

	@Transactional(readOnly = true)
	public TorikumiFormDTO getForm(Integer bashoId, Integer torikumiId) {
		BashoEntity basho = findBasho(bashoId);
		TorikumiEntity t = findMatchInBasho(bashoId, torikumiId);

		TorikumiFormDTO form = new TorikumiFormDTO();
		form.setId(t.getId());
		form.setBashoLabelKr(bashoLabel(basho));
		form.setDay(t.getDay());
		form.setDivision(t.getDivision());
		form.setEastRikishiId(t.getEastRikishiEntity().getId());
		form.setWestRikishiId(t.getWestRikishiEntity().getId());
		form.setWinnerSide(winnerSideOf(t) != null ? winnerSideOf(t).name() : "");
		form.setResultType(t.getResultType());
		form.setKimarite(KimariteDisplayUtil.toKr(t.getKimarite()));
		form.setExtraMatch(t.isExtraMatch());
		form.setYoutubeUrl(t.getYoutubeUrl());
		form.setDescriptionKr(t.getDescriptionKr());
		form.setDescriptionJp(t.getDescriptionJp());
		return form;
	}

	@Transactional(readOnly = true)
	public List<RikishiOptionDTO> getRikishiOptions() {
		return rikishiRepository.findAll(Sort.by("shikonaKr")).stream()
				.map(r -> new RikishiOptionDTO(r.getId(), r.getShikonaKr(), r.isActive()))
				.toList();
	}

	public List<String> getKimariteSuggestions() {
		return KimariteDisplayUtil.krSuggestions();
	}

	@Transactional
	public void addMatch(Integer bashoId, TorikumiFormDTO form) {
		BashoEntity basho = findBasho(bashoId);
		Parsed p = validateAndResolve(form);

		if (torikumiRepository.existsByBashoEntityIdAndDayAndEastRikishiEntityIdAndWestRikishiEntityIdAndIsExtraMatch(
				basho.getId(), p.day, p.east.getId(), p.west.getId(), form.isExtraMatch())) {
			throw new IllegalArgumentException("이미 등록된 대전입니다 (같은 날·동서 조합).");
		}

		torikumiRepository.save(TorikumiEntity.builder()
				.bashoEntity(basho)
				.day(p.day)
				.division(form.getDivision())
				.eastRikishiEntity(p.east)
				.westRikishiEntity(p.west)
				.winnerRikishiEntity(p.winner)
				.loserRikishiEntity(p.loser)
				.resultType(p.resultType)
				.kimarite(p.kimarite)
				.isExtraMatch(form.isExtraMatch())
				.youtubeUrl(blankToNull(form.getYoutubeUrl()))
				.descriptionKr(blankToNull(form.getDescriptionKr()))
				.descriptionJp(blankToNull(form.getDescriptionJp()))
				.build());
	}

	@Transactional
	public void updateMatch(Integer bashoId, Integer torikumiId, TorikumiFormDTO form) {
		findBasho(bashoId);
		TorikumiEntity t = findMatchInBasho(bashoId, torikumiId);
		Parsed p = validateAndResolve(form);

		t.updateMatch(
				p.day, form.getDivision(),
				p.east, p.west, p.winner, p.loser,
				p.resultType, p.kimarite, form.isExtraMatch(),
				blankToNull(form.getYoutubeUrl()),
				blankToNull(form.getDescriptionKr()),
				blankToNull(form.getDescriptionJp())
		);
	}

	@Transactional
	public void deleteMatch(Integer bashoId, Integer torikumiId) {
		findBasho(bashoId);
		torikumiRepository.delete(findMatchInBasho(bashoId, torikumiId));
	}

	// ===== 내부 =====

	/** 검증을 통과한 폼에서 뽑아낸 엔티티 참조들. */
	private record Parsed(int day, RikishiEntity east, RikishiEntity west,
						  RikishiEntity winner, RikishiEntity loser,
						  ResultType resultType, String kimarite) {
	}

	private Parsed validateAndResolve(TorikumiFormDTO form) {
		if (form.getDay() == null || form.getDay() < 1) {
			throw new IllegalArgumentException("날짜(일차)는 1 이상이어야 합니다.");
		}
		if (form.getDivision() == null) {
			throw new IllegalArgumentException("디비전은 필수입니다.");
		}
		if (form.getEastRikishiId() == null || form.getWestRikishiId() == null) {
			throw new IllegalArgumentException("동/서 리키시를 모두 선택하세요.");
		}
		if (form.getEastRikishiId().equals(form.getWestRikishiId())) {
			throw new IllegalArgumentException("동/서 리키시가 같을 수 없습니다.");
		}

		RikishiEntity east = rikishiRepository.findById(form.getEastRikishiId())
				.orElseThrow(() -> new EntityNotFoundException("리키시를 찾을 수 없습니다. id=" + form.getEastRikishiId()));
		RikishiEntity west = rikishiRepository.findById(form.getWestRikishiId())
				.orElseThrow(() -> new EntityNotFoundException("리키시를 찾을 수 없습니다. id=" + form.getWestRikishiId()));

		RikishiEntity winner = null;
		RikishiEntity loser = null;
		String ws = form.getWinnerSide();
		if ("EAST".equals(ws)) {
			winner = east;
			loser = west;
		} else if ("WEST".equals(ws)) {
			winner = west;
			loser = east;
		} else if (ws != null && !ws.isBlank()) {
			throw new IllegalArgumentException("승자 값이 올바르지 않습니다.");
		}

		ResultType resultType = (form.getResultType() == null) ? ResultType.NORMAL : form.getResultType();
		// 부전승패는 결정기술이 없다
		String kimarite = (resultType == ResultType.FUZEN) ? null : blankToNull(form.getKimarite());

		return new Parsed(form.getDay(), east, west, winner, loser, resultType, kimarite);
	}

	private BashoEntity findBasho(Integer bashoId) {
		return bashoRepository.findById(bashoId)
				.orElseThrow(() -> new EntityNotFoundException("바쇼를 찾을 수 없습니다. id=" + bashoId));
	}

	private TorikumiEntity findMatchInBasho(Integer bashoId, Integer torikumiId) {
		TorikumiEntity t = torikumiRepository.findById(torikumiId)
				.orElseThrow(() -> new EntityNotFoundException("대전을 찾을 수 없습니다. id=" + torikumiId));
		if (!t.getBashoEntity().getId().equals(bashoId)) {
			throw new EntityNotFoundException("해당 바쇼의 대전이 아닙니다. id=" + torikumiId);
		}
		return t;
	}

	private static Side winnerSideOf(TorikumiEntity t) {
		if (t.getWinnerRikishiEntity() == null) {
			return null;
		}
		return t.getWinnerRikishiEntity().getId().equals(t.getEastRikishiEntity().getId())
				? Side.EAST : Side.WEST;
	}

	private static TorikumiRowDTO toRow(TorikumiEntity t) {
		return new TorikumiRowDTO(
				t.getId(),
				t.getDay(),
				t.getDivision(),
				t.getEastRikishiEntity().getId(),
				t.getEastRikishiEntity().getShikonaKr(),
				t.getWestRikishiEntity().getId(),
				t.getWestRikishiEntity().getShikonaKr(),
				winnerSideOf(t),
				t.getResultType(),
				KimariteDisplayUtil.toKr(t.getKimarite()),
				t.isExtraMatch(),
				t.getYoutubeUrl() != null && !t.getYoutubeUrl().isBlank()
		);
	}

	private static String bashoLabel(BashoEntity basho) {
		return basho.getBashoYear() + "년 " + basho.getBashoMonth().getMonthValue() + "월 "
				+ basho.getBashoMonth().getDisplayNameKr();
	}

	private static String blankToNull(String value) {
		return (value == null || value.isBlank()) ? null : value.strip();
	}
}
