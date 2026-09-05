package com.torikumilab.sumoarchive.domain.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 리키시 상세 프로필 화면(rikishi/detail.html)에 뿌려주는 DTO.
 * RikishiDetailService#getRikishiDetail() 에서 조립됩니다.
 */
@Getter
@Builder
public class RikishiDetailDTO {

	private Integer id;

	private String shikonaKr;
	private String shikonaJp;
	private String name; // 본명

	private LocalDate birthdate;
	private Integer age; // 서버에서 계산해서 내려줌 (Period.between)

	private String birthplace;
	private String nationality;

	private String heyaNameKr; // 무소속(하쿠호 등)이면 null

	private BigDecimal height;
	private BigDecimal weight;

	private String fightingStyle; // 관리자 입력값, 없으면 null → 화면에서 "미분류" 처리

	private LocalDate debutDate;
	private String highestRank;

	private boolean isActive;
	private LocalDate retiredDate;     // 현역이면 null
	private String oyakataNameKr;      // 오야카타 아니면 null

	private String photoUrl; // 없으면 null → 화면에서 시코나 첫 글자 플레이스홀더로 대체

	// 최신 반즈케 기준 표시용 (은퇴/오야카타면 null)
	private String sideDisplay; // "東" / "西"
	private String rankDisplay; // "前頭10枚目", "横綱" 등

	private List<ShikonaHistoryItemDTO> shikonaHistory; // 과거 시코나만, 오래된 순

	// --- 커리어 레코드 ---
	private long totalWins;
	private long totalLosses;
	private long totalAbsences; // ⚠ 현재 스키마는 휴장(休場) 기록이 없어 항상 0. 필요하면 스키마 추가 논의 필요
	private long totalBasho;

	private long yushoMakuuchi;
	private long yushoJuryo;
	private long sanshoShukun; // 殊勲賞
	private long sanshoKanto;  // 敢闘賞
	private long sanshoGino;   // 技能賞
	private long kinboshiCount;
}
