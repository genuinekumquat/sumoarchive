package com.torikumilab.sumoarchive.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.transaction.TransactionAwareCacheManagerProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 인메모리 캐시(반즈케·바쇼 목록·일문 구조·키마리테 백과사전)를 트랜잭션 커밋에 맞춰 비운다.
 *
 * <p>관리자 수정 메서드는 @Transactional + @CacheEvict인데, 기본 캐시 매니저는 메서드가 끝나는 즉시
 * (커밋 전일 수 있음) 캐시를 비운다. 그 사이 메인 페이지 요청이 들어오면 커밋 전의 옛 데이터를 다시
 * 캐시에 담아 다음 수정 전까지 옛 화면이 보인다. TransactionAwareCacheManagerProxy로 감싸면 비우기가
 * 커밋 직후로 미뤄지고, 트랜잭션 밖(BashoBatchImportService 등)에서는 지금처럼 바로 비운다.</p>
 */
@Configuration
public class CacheConfig {

	@Bean
	public CacheManager cacheManager() {
		return new TransactionAwareCacheManagerProxy(new ConcurrentMapCacheManager());
	}
}
