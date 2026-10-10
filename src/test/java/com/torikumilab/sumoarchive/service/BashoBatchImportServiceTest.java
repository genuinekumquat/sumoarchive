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
	@DisplayName("단일 바쇼 일괄 임포트 - 6개 디비전 반즈케·토리쿠미와 수상 데이터를 순차 동기화")
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

		// 디비전별 반즈케 인원 / 경기 수 (마쿠우치·쥬료 + 하위 4개)
		int[] banzukeSizes = {42, 28, 120, 160, 200, 43};
		int[] boutCounts = {300, 200, 395, 531, 662, 125};
		Division[] divisions = Division.values();
		for (int i = 0; i < divisions.length; i++) {
			given(banzukeImportService.importDivision(1, divisions[i])).willReturn(
					new BanzukeImportResultDTO("2025년 1월", divisions[i], banzukeSizes[i], 0, List.of()));
			given(torikumiImportService.importDivision(1, divisions[i])).willReturn(
					new TorikumiImportResultDTO("2025년 1월", divisions[i], boutCounts[i], 0, 15, boutCounts[i], List.of()));
		}

		AwardImportResultDTO awardRes = new AwardImportResultDTO("2025년 1월", 2, 3, 1, List.of());
		given(awardImportService.importForBasho(1)).willReturn(awardRes);

		// when
		BashoBatchImportService.BashoBatchResult result = bashoBatchImportService.importBashoFull(1);

		// then
		for (Division d : divisions) {
			verify(banzukeImportService).importDivision(1, d);
			verify(torikumiImportService).importDivision(1, d);
		}
		assertThat(result.warnings()).isEmpty();
		assertThat(result.banzukeCreated()).isEqualTo(593);
		assertThat(result.torikumiCreated()).isEqualTo(2213);
		assertThat(result.yushoCreated()).isEqualTo(2);
		assertThat(result.sanshoCreated()).isEqualTo(3);
		assertThat(result.kinboshiCreated()).isEqualTo(1);
	}
}
