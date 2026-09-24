package com.torikumilab.sumoarchive.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Disabled("2025 데이터 일괄 임포트 실행용 통합 테스트 (필요 시 수동 실행)")
class Import2025IntegrationTest {

	@Autowired
	private BashoBatchImportService bashoBatchImportService;

	@Test
	void runImport2025() {
		BashoBatchImportService.YearBatchResult result = bashoBatchImportService.importYear(2025);
		System.out.println("2025 Import Result: " + result);
		assertThat(result.totalBashos()).isEqualTo(6);
		assertThat(result.totalTorikumiCreated()).isGreaterThan(1000);
	}
}
