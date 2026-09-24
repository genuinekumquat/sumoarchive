package com.torikumilab.sumoarchive.service;

import com.torikumilab.sumoarchive.domain.dto.AwardImportResultDTO;
import com.torikumilab.sumoarchive.domain.dto.BanzukeImportResultDTO;
import com.torikumilab.sumoarchive.domain.dto.TorikumiImportResultDTO;
import com.torikumilab.sumoarchive.domain.entity.BashoEntity;
import com.torikumilab.sumoarchive.domain.entity.constant.BashoMonth;
import com.torikumilab.sumoarchive.domain.entity.constant.Division;
import com.torikumilab.sumoarchive.repository.BashoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BashoBatchImportServiceTest {

	@Mock
	private BashoRepository bashoRepository;
	@Mock
	private BanzukeImportService banzukeImportService;
	@Mock
	private TorikumiImportService torikumiImportService;
	@Mock
	private AwardImportService awardImportService;

	@InjectMocks
	private BashoBatchImportService bashoBatchImportService;

	@Test
	@DisplayName("단일 바쇼 일괄 임포트 - 마쿠우치 및 쥬료 반즈케, 토리쿠미, 수상 데이터 순차 동기화")
	void importBashoFull() {
		// given
		BashoEntity basho = BashoEntity.builder()
				.bashoYear(2025)
				.bashoMonth(BashoMonth.JAN)
				.startDate(LocalDate.of(2025, 1, 12))
				.endDate(LocalDate.of(2025, 1, 26))
				.externalBashoId("202501")
				.build();
		given(bashoRepository.findById(1)).willReturn(Optional.of(basho));

		BanzukeImportResultDTO bzMak = new BanzukeImportResultDTO("2025년 1월", Division.Makuuchi, 42, 0, List.of());
		BanzukeImportResultDTO bzJur = new BanzukeImportResultDTO("2025년 1월", Division.Juryo, 28, 0, List.of());
		given(banzukeImportService.importDivision(1, Division.Makuuchi)).willReturn(bzMak);
		given(banzukeImportService.importDivision(1, Division.Juryo)).willReturn(bzJur);

		TorikumiImportResultDTO tkMak = new TorikumiImportResultDTO("2025년 1월", Division.Makuuchi, 300, 0, 15, 300, List.of());
		TorikumiImportResultDTO tkJur = new TorikumiImportResultDTO("2025년 1월", Division.Juryo, 200, 0, 15, 200, List.of());
		given(torikumiImportService.importDivision(1, Division.Makuuchi)).willReturn(tkMak);
		given(torikumiImportService.importDivision(1, Division.Juryo)).willReturn(tkJur);

		AwardImportResultDTO awardRes = new AwardImportResultDTO("2025년 1월", 2, 3, 1, List.of());
		given(awardImportService.importForBasho(1)).willReturn(awardRes);

		// when
		BashoBatchImportService.BashoBatchResult result = bashoBatchImportService.importBashoFull(1);

		// then
		assertThat(result.banzukeCreated()).isEqualTo(70);
		assertThat(result.torikumiCreated()).isEqualTo(500);
		assertThat(result.yushoCreated()).isEqualTo(2);
		assertThat(result.sanshoCreated()).isEqualTo(3);
		assertThat(result.kinboshiCreated()).isEqualTo(1);
	}
}
