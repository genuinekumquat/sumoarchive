package com.torikumilab.sumoarchive.client;

import com.torikumilab.sumoarchive.domain.dto.sumoapi.SumoApiBanzukeDTO;
import com.torikumilab.sumoarchive.domain.dto.sumoapi.SumoApiBashoDTO;
import com.torikumilab.sumoarchive.domain.dto.sumoapi.SumoApiRikishiPageDTO;
import com.torikumilab.sumoarchive.domain.entity.constant.Division;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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

		this.rc = RestClient.builder()
				.baseUrl(baseUrl)
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
}
