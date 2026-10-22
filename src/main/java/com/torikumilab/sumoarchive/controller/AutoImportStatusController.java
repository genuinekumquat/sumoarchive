package com.torikumilab.sumoarchive.controller;

import com.torikumilab.sumoarchive.service.BashoAutoImportScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 자동 임포트 감시용 (/status/auto-import). 외부 업타임 모니터(UptimeRobot)가 5분마다 확인해서
 * 503이면 메일을 보낸다 — 앱에 메일 설정·비밀번호를 두지 않고 사이트 감시와 같은 알림 경로를 쓰기 위해.
 *
 * <p>공개 주소라 오류 내용은 싣지 않고 판정·시각·건수만 보여준다. 자세한 경고는 관리자 바쇼 화면.</p>
 */
@RestController
@RequiredArgsConstructor
public class AutoImportStatusController {

	private final BashoAutoImportScheduler bashoAutoImportScheduler;

	@GetMapping(value = "/status/auto-import", produces = MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8")
	public ResponseEntity<String> status() {
		BashoAutoImportScheduler.Alert alert = bashoAutoImportScheduler.alert();
		return ResponseEntity.status(alert.ok() ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
				.body(alert.message());
	}
}
