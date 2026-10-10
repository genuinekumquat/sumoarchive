package com.torikumilab.sumoarchive.client;

import com.torikumilab.sumoarchive.domain.dto.sumoapi.SumoApiBanzukeDTO;
import com.torikumilab.sumoarchive.domain.dto.sumoapi.SumoApiBashoDTO;
import com.torikumilab.sumoarchive.domain.dto.sumoapi.SumoApiRikishiDTO;
import com.torikumilab.sumoarchive.domain.dto.sumoapi.SumoApiRikishiPageDTO;
import com.torikumilab.sumoarchive.domain.dto.sumoapi.SumoApiTorikumiDayDTO;
import com.torikumilab.sumoarchive.domain.entity.constant.Division;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

/**
 * sumo-api.com 호출 래퍼. 인증 없음. base-url은 application.properties의 {@code sumo-api.base-url}.
 *
 * <p>⚠ sumo-api는 JSON 본문을 {@code Content-Type: text/plain}으로 내려주므로,
 * Jackson 컨버터가 {@code text/plain}도 받도록 별도로 구성한다.</p>
 */
@Component
public class SumoApiClient {

	private final RestClient rc;

	public SumoApiClient(@Value("${sumo-api.base-url}") String baseUrl) {
		MappingJackson2HttpMessageConverter jackson = new MappingJackson2HttpMessageConverter();
		jackson.setSupportedMediaTypes(List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN));

		// 기본값은 제한 시간이 없어서 응답이 멈추면 임포트 요청 전체가 걸려 있게 된다.
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(Duration.ofSeconds(10));
		requestFactory.setReadTimeout(Duration.ofSeconds(30));

		this.rc = RestClient.builder()
				.baseUrl(baseUrl)
				.requestFactory(requestFactory)
				.messageConverters(converters -> {
					converters.removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);
					converters.add(jackson);
				})
				.build();
	}

	/**
	 * 현역 리키시 페이지 조회. {@code intai=false}면 현역만(약 601명), 페이징은 skip/limit.
	 */
	public SumoApiRikishiPageDTO getActiveRikishis(int limit, int skip) {
		return rc.get()
				.uri("/api/rikishis?limit={limit}&skip={skip}&intai=false", limit, skip)
				.retrieve()
				.body(SumoApiRikishiPageDTO.class);
	}

	/**
	 * 특정 리키시({@code id}) 단건 조회. 은퇴자 포함. 없거나 실패 시 null.
	 * 하위 리그 과거 바쇼를 처음 적재하면 은퇴 선수를 수천 번 연달아 조회하게 되므로, 일시적인 오류(429·5xx·
	 * 연결 끊김)는 잠깐 쉬었다가 다시 시도한다 - 실패하면 그 선수의 반즈케·경기가 통째로 빠지기 때문.
	 * 404(없는 선수)는 다시 시도해도 같으니 바로 null.
	 */
	public SumoApiRikishiDTO getRikishi(int id) {
		for (int attempt = 1; ; attempt++) {
			try {
				return rc.get()
						.uri("/api/rikishi/{id}", id)
						.retrieve()
						.body(SumoApiRikishiDTO.class);
			} catch (HttpClientErrorException.NotFound e) {
				return null;
			} catch (RuntimeException e) {
				if (attempt >= RIKISHI_MAX_ATTEMPTS) {
					return null;
				}
				sleepQuietly(500L * attempt);
			}
		}
	}

	private static final int RIKISHI_MAX_ATTEMPTS = 3;

	private static void sleepQuietly(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * 특정 바쇼({@code YYYYMM}) 조회. 없는 바쇼면 4xx가 오므로 호출부에서 예외를 잡아 스킵한다.
	 */
	public SumoApiBashoDTO getBasho(String yyyymm) {
		return rc.get()
				.uri("/api/basho/{id}", yyyymm)
				.retrieve()
				.body(SumoApiBashoDTO.class);
	}

	/**
	 * 특정 바쇼({@code YYYYMM})의 디비전 반즈케 조회. {@code east[]}/{@code west[]}로 나뉘어 온다.
	 * 디비전 이름은 우리 {@link Division} enum과 철자가 완전히 일치한다.
	 */
	public SumoApiBanzukeDTO getBanzuke(String yyyymm, Division division) {
		return rc.get()
				.uri("/api/basho/{id}/banzuke/{division}", yyyymm, division.name())
				.retrieve()
				.body(SumoApiBanzukeDTO.class);
	}

	/**
	 * 특정 바쇼({@code YYYYMM})·디비전·일차의 대전 조회. 아직 안 치러진 일차는 {@code torikumi}가
	 * null인 응답이 온다(4xx 아님).
	 */
	public SumoApiTorikumiDayDTO getTorikumi(String yyyymm, Division division, int day) {
		return rc.get()
				.uri("/api/basho/{id}/torikumi/{division}/{day}", yyyymm, division.name(), day)
				.retrieve()
				.body(SumoApiTorikumiDayDTO.class);
	}
}
