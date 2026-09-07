package com.torikumilab.sumoarchive.domain.dto;

/** 관리자 반즈케 행 추가 폼의 리키시 드롭다운 옵션. active로 현역/비현역을 화면에서 구분 표기한다. */
public record RikishiOptionDTO(Integer id, String shikonaKr, boolean active) {
}
