package com.torikumilab.sumoarchive.domain.dto;

/**
 * 메인 팝업에 띄울 공지 한 건 (요청 언어로 고른 문구). id는 방문자 브라우저의 "다시 보지 않기" 기억용.
 */
public record NoticeDTO(Integer id, String title, String body, String linkUrl) {
}
