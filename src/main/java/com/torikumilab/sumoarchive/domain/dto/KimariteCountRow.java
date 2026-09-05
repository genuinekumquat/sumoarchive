package com.torikumilab.sumoarchive.domain.dto;

/**
 * TorikumiRepository#findKimariteStats() 의 raw 집계 결과 (Spring Data 인터페이스 프로젝션).
 * 승수 기준으로 내림차순 정렬해서 내려옴.
 */
public interface KimariteCountRow {
	String getKimarite();
	Long getCnt();
}
