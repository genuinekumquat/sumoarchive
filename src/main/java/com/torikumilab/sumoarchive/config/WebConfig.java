package com.torikumilab.sumoarchive.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.util.Locale;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new AdminAuthInterceptor())
				.addPathPatterns("/admin/**")
				.excludePathPatterns("/admin/login", "/admin/logout");
		// 로그인 확인 다음에 CSRF 토큰 확인. 댓글 블라인드 API는 /admin 밖이지만 관리자 전용이라 같이 건다.
		registry.addInterceptor(new AdminCsrfInterceptor())
				.addPathPatterns("/admin/**", "/api/torikumi/*/comments/*/blind")
				.excludePathPatterns("/admin/login", "/admin/logout");
		registry.addInterceptor(localeChangeInterceptor());
	}

	/** th:action 폼에 관리자 CSRF 토큰 숨김 필드를 자동으로 붙인다. 빈 이름은 Spring MVC 규약상 고정. */
	@Bean(name = "requestDataValueProcessor")
	public CsrfRequestDataValueProcessor requestDataValueProcessor() {
		return new CsrfRequestDataValueProcessor();
	}

	// 세션에 언어를 기억해둔다 (기본 한국어). 메인 페이지 언어 스위처(?lang=ja/ko)가 이걸 바꿔줌.
	@Bean
	public LocaleResolver localeResolver() {
		SessionLocaleResolver resolver = new SessionLocaleResolver();
		resolver.setDefaultLocale(Locale.KOREAN);
		return resolver;
	}

	@Bean
	public LocaleChangeInterceptor localeChangeInterceptor() {
		LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
		interceptor.setParamName("lang");
		return interceptor;
	}
}
