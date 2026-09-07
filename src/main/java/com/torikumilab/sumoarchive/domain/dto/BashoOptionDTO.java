package com.torikumilab.sumoarchive.domain.dto;

/**
 * 메인 화면 반즈케 패널의 바쇼 선택 드롭다운 옵션.
 * label 예: "2026年 秋場所" (연도+대회 합친 표기), monthLabel 예: "秋場所" (대회만 — 연/대회 분리 드롭다운용).
 */
public record BashoOptionDTO(Integer id, String label, int year, int month, String monthLabel) {
}
