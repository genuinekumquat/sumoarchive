package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.domain.dto.BanzukeBoardDTO;
import com.torikumilab.sumoarchive.domain.dto.BanzukeFormDTO;
import com.torikumilab.sumoarchive.domain.dto.BanzukeRowDTO;
import com.torikumilab.sumoarchive.domain.dto.BashoAdminRowDTO;
import com.torikumilab.sumoarchive.domain.dto.BashoCreateFormDTO;
import com.torikumilab.sumoarchive.domain.dto.RikishiOptionDTO;
import com.torikumilab.sumoarchive.domain.entity.BanzukeEntity;
import com.torikumilab.sumoarchive.domain.entity.BashoEntity;
import com.torikumilab.sumoarchive.domain.entity.RikishiEntity;
import com.torikumilab.sumoarchive.domain.entity.constant.Division;
import com.torikumilab.sumoarchive.repository.BanzukeRepository;
import com.torikumilab.sumoarchive.repository.BashoRepository;
import com.torikumilab.sumoarchive.repository.RikishiRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 관리자 "데이터 수동 갱신" 중 반즈케(番付) 관리. 새 바쇼 생성 + 그 바쇼의 반즈케 행을
 * 리키시 한 명씩 추가/수정/삭제한다. /admin/** 경로라 AdminAuthInterceptor가 세션 isAdmin을
 * 먼저 검사한다.
 *
 * <p>RikishiEntity.currentRank(현재 계급 캐시)는 건드리지 않는다 — 어디서도 읽히지 않는 죽은
 * 필드이고, 화면에 보이는 계급/동서는 전부 이 banzuke 테이블에서 온다.</p>
 */
@Service
@RequiredArgsConstructor
public class BanzukeAdminService {

	private final BashoRepository bashoRepository;
	private final BanzukeRepository banzukeRepository;
	private final RikishiRepository rikishiRepository;

	// ===== 바쇼 =====

	@Transactional(readOnly = true)
	public List<BashoAdminRowDTO> listBashos() {
		return bashoRepository.findAllByOrderByStartDateDesc().stream()
				.map(b -> new BashoAdminRowDTO(
						b.getId(),
						b.getBashoYear(),
						b.getBashoMonth().getDisplayNameKr(),
						b.getBashoMonth().getMonthValue(),
						b.getStartDate(),
						b.getEndDate(),
						banzukeRepository.countByBashoEntityId(b.getId())
				))
				.toList();
	}

	@Transactional
	public void createBasho(BashoCreateFormDTO form) {
		if (form.getBashoYear() == null || form.getBashoMonth() == null) {
			throw new IllegalArgumentException("연도와 월은 필수입니다.");
		}
		if (form.getStartDate() == null || form.getEndDate() == null) {
			throw new IllegalArgumentException("시작일과 종료일은 필수입니다.");
		}
		if (form.getEndDate().isBefore(form.getStartDate())) {
			throw new IllegalArgumentException("종료일이 시작일보다 앞설 수 없습니다.");
		}
		if (bashoRepository.existsByBashoYearAndBashoMonth(form.getBashoYear(), form.getBashoMonth())) {
			throw new IllegalArgumentException("이미 등록된 바쇼입니다.");
		}
		bashoRepository.save(BashoEntity.builder()
				.bashoYear(form.getBashoYear())
				.bashoMonth(form.getBashoMonth())
				.startDate(form.getStartDate())
				.endDate(form.getEndDate())
				.build());
	}

	// ===== 반즈케 행 =====

	@Transactional(readOnly = true)
	public BanzukeBoardDTO getBoard(Integer bashoId, Division division) {
		BashoEntity basho = findBasho(bashoId);
		List<BanzukeRowDTO> rows = banzukeRepository
				.findByBashoAndDivisionOrdered(basho.getId(), division)
				.stream()
				.map(b -> new BanzukeRowDTO(
						b.getId(),
						b.getRikishiEntity().getId(),
						b.getRikishiEntity().getShikonaKr(),
						b.getRikishiEntity().getShikonaJp(),
						b.getRankName(),
						b.getSide(),
						b.getRankValue()
				))
				.toList();
		return new BanzukeBoardDTO(basho.getId(), bashoLabel(basho), division, rows);
	}

	@Transactional(readOnly = true)
	public List<RikishiOptionDTO> getRikishiOptions() {
		return rikishiRepository.findAll(Sort.by("shikonaKr")).stream()
				.map(r -> new RikishiOptionDTO(r.getId(), r.getShikonaKr(), r.isActive()))
				.toList();
	}

	@Transactional
	public void addRow(Integer bashoId, BanzukeFormDTO form) {
		BashoEntity basho = findBasho(bashoId);
		validatePlacement(form);
		if (form.getRikishiId() == null) {
			throw new IllegalArgumentException("리키시를 선택하세요.");
		}
		RikishiEntity rikishi = rikishiRepository.findById(form.getRikishiId())
				.orElseThrow(() -> new EntityNotFoundException("리키시를 찾을 수 없습니다. id=" + form.getRikishiId()));
		if (banzukeRepository.existsByBashoEntityIdAndRikishiEntityId(basho.getId(), rikishi.getId())) {
			throw new IllegalArgumentException("이미 이 바쇼에 반즈케가 등록된 리키시입니다.");
		}
		banzukeRepository.save(BanzukeEntity.builder()
				.rikishiEntity(rikishi)
				.bashoEntity(basho)
				.division(form.getDivision())
				.rankName(form.getRankName())
				.side(form.getSide())
				.rankValue(form.getRankValue())
				.build());
	}

	@Transactional
	public void updateRow(Integer bashoId, Integer banzukeId, BanzukeFormDTO form) {
		validatePlacement(form);
		BanzukeEntity row = findRowInBasho(bashoId, banzukeId);
		row.updatePlacement(form.getDivision(), form.getRankName(), form.getSide(), form.getRankValue());
	}

	@Transactional
	public void deleteRow(Integer bashoId, Integer banzukeId) {
		BanzukeEntity row = findRowInBasho(bashoId, banzukeId);
		banzukeRepository.delete(row);
	}

	// ===== 내부 =====

	private BashoEntity findBasho(Integer bashoId) {
		return bashoRepository.findById(bashoId)
				.orElseThrow(() -> new EntityNotFoundException("바쇼를 찾을 수 없습니다. id=" + bashoId));
	}

	private BanzukeEntity findRowInBasho(Integer bashoId, Integer banzukeId) {
		BanzukeEntity row = banzukeRepository.findById(banzukeId)
				.orElseThrow(() -> new EntityNotFoundException("반즈케 행을 찾을 수 없습니다. id=" + banzukeId));
		if (!row.getBashoEntity().getId().equals(bashoId)) {
			throw new EntityNotFoundException("해당 바쇼의 반즈케 행이 아닙니다. id=" + banzukeId);
		}
		return row;
	}

	private void validatePlacement(BanzukeFormDTO form) {
		if (form.getDivision() == null || form.getRankName() == null || form.getSide() == null) {
			throw new IllegalArgumentException("디비전 / 계급 / 동서는 필수입니다.");
		}
	}

	private static String bashoLabel(BashoEntity basho) {
		return basho.getBashoYear() + "년 " + basho.getBashoMonth().getMonthValue() + "월 "
				+ basho.getBashoMonth().getDisplayNameKr();
	}
}
