package com.torikumilab.sumoarchive;

import com.torikumilab.sumoarchive.domain.entity.*;
import com.torikumilab.sumoarchive.domain.entity.constant.*;
import com.torikumilab.sumoarchive.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * ⚠ 데이터 출처 안내
 * - 요코즈나/오제키/세키와케/코무스비급 및 유명 마에가시라(타카야스, 미타케우미, 다이에이쇼,
 *   와카타카카게, 쇼다이, 토비자루, 치요쇼마, 아비 등)의 생년월일/신장/체중/본명/최고위는
 *   실제 알려진 정보를 최대한 반영했습니다.
 * - 그 외 마에가시라 다수(고노야마, 후지노카와, 하쿠노후지, 이치야마모토, 후지세이운,
 *   코토에이호, 로가, 후지료가, 와카노쇼, 아사하쿠류, 킨보잔, 시시, 오노카츠, 카즈마,
 *   다이세이잔, 아사코류 등)는 본명/신장/체중/생년월일이 추정치이며, 시코나 한자표기도
 *   일부 추정입니다. 발표 전 확인 권장.
 * - 이치몬(一門) 매핑도 검색 기반 추정입니다 (특히 오노마츠·후지시마·나루토 등).
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
	
	private final HeyaRepository heyaRepository;
	private final RikishiRepository rikishiRepository;
	private final BashoRepository bashoRepository;
	private final TorikumiRepository torikumiRepository;
	private final BanzukeRepository banzukeRepository;
	private final RikishiShikonaHistoryRepository rikishiShikonaHistoryRepository;
	
	// 외부 API ID 채번용 (1001~1006은 기존 6명이 사용 중이므로 2001부터 시작)
	private int nextExternalId = 2001;
	
	// 토리쿠미 더미데이터용 결정기술(키마리테) 후보 - 임의 생성 시 여기서 뽑아 씀
	private static final List<String> KIMARITE_POOL = List.of(
			"요리키리", "오시다시", "하타키코미", "츠키다시", "요리타오시", "히키오토시",
			"우와테나게", "시타테나게", "오시타오시", "츠리다시", "카케나게", "소토가케",
			"우치가케", "슷타리", "요비모도시", "코테나게"
	);
	
	// 더미데이터 재현성을 위해 고정 시드 사용 (앱 재기동 시에도 같은 결과)
	private final Random random = new Random(20260712L);
	
	@Override
	@Transactional
	public void run(String... args) throws Exception {
		if (heyaRepository.count() > 0) {
			return;
		}
		
		// =====================================================
		// 1. 헤야
		//    - 명명규칙 변경: "OO 헤야"/"OO部屋" 대신 "OO"만 사용 (한/일 동일 적용)
		//    - 2026년 7월 나고야바쇼 마쿠우치 반즈케(36명)에 필요한 헤야를 모두 생성
		// =====================================================
		
		// 1-1. 미야기노 - 하쿠호(당시 미야기노 오야카타) 소속 리키시의 폭력사건 감독 책임으로
		//      2024년경 폐쇄, 소속 리키시는 이세가하마 헤야로 이적 (실제 사건 기반으로 반영)
//		HeyaEntity miyaginoHeya = HeyaEntity.builder()
//				.nameKr("미야기노")
//				.nameJp("宮城野")
//				.ichimonKr("이세가하마 이치몬")
//				.ichimonJp("伊勢ヶ濱一門")
//				.isActive(false)
//				.dissolvedDate(LocalDate.of(2024, 4, 1)) // 실제 폐쇄 시점 추정치
//				.build();
//		miyaginoHeya = heyaRepository.save(miyaginoHeya);
		
		// 1-2. 이세가하마 - 테루노후지가 은퇴 후 명적을 계승해 신설 사장이 된 헤야
		//      (구 미야기노 소속 리키시들도 실제로는 이곳으로 이적했으나, 현재 시더 범위에서는
		//       테루노후지 본인만 반영. 나머지는 다음 세션에서 필요 시 추가)
		HeyaEntity isegahamaHeya = heya("이세가하마", "伊勢ヶ濱", "이세가하마 이치몬", "伊勢ヶ濱一門");
		
		HeyaEntity takasagoHeya = heya("타카사고", "高砂", "타카사고 이치몬", "高砂一門");
		HeyaEntity sadogatakeHeya = heya("사도가타케", "佐渡ヶ嶽", "니쇼노세키 이치몬", "二所ノ関一門");
		
		HeyaEntity tatsunamiHeya = heya("타츠나미", "立浪", "데와노우미 이치몬", "出羽海一門");
		HeyaEntity nishonosekiHeya = heya("니쇼노세키", "二所ノ関", "니쇼노세키 이치몬", "二所ノ関一門");
		HeyaEntity otowayamaHeya = heya("오토와야마", "音羽山", "니쇼노세키 이치몬", "二所ノ関一門");
		HeyaEntity arashioHeya = heya("아라시오", "荒汐", "데와노우미 이치몬", "出羽海一門");
		HeyaEntity ajigawaHeya = heya("아지가와", "安治川", "데와노우미 이치몬", "出羽海一門");
		HeyaEntity isenoumiHeya = heya("이세노우미", "伊勢ノ海", "데와노우미 이치몬", "出羽海一門");
		HeyaEntity otakeHeya = heya("오타케", "大嶽", "니쇼노세키 이치몬", "二所ノ関一門");
		HeyaEntity minatogawaHeya = heya("미나토가와", "湊川", "니쇼노세키 이치몬", "二所ノ関一門");
		HeyaEntity takekumaHeya = heya("타케쿠마", "武隈", "데와노우미 이치몬", "出羽海一門");
		HeyaEntity kiseHeya = heya("키세", "木瀬", "데와노우미 이치몬", "出羽海一門");
		HeyaEntity sakaigawaHeya = heya("사카이가와", "境川", "데와노우미 이치몬", "出羽海一門");
		HeyaEntity oitekazeHeya = heya("오이테카제", "追手風", "토키츠카제 이치몬", "時津風一門");
		HeyaEntity hanaregomaHeya = heya("하나레고마", "放駒", "니쇼노세키 이치몬", "二所ノ関一門");
		HeyaEntity narutoHeya = heya("나루토", "鳴戸", "니쇼노세키 이치몬", "二所ノ関一門");
		HeyaEntity tokitsukazeHeya = heya("토키츠카제", "時津風", "토키츠카제 이치몬", "時津風一門");
		HeyaEntity fujishimaHeya = heya("후지시마", "藤島", "데와노우미 이치몬", "出羽海一門");
		HeyaEntity tagonouraHeya = heya("타고노우라", "田子ノ浦", "니쇼노세키 이치몬", "二所ノ関一門");
		HeyaEntity futagoyamaHeya = heya("후타고야마", "二子山", "데와노우미 이치몬", "出羽海一門");
		HeyaEntity kokonoeHeya = heya("코코노에", "九重", "데와노우미 이치몬", "出羽海一門");
		HeyaEntity dewanoumiHeya = heya("데와노우미", "出羽海", "데와노우미 이치몬", "出羽海一門");
		HeyaEntity shikoroyamaHeya = heya("시코로야마", "錣山", "니쇼노세키 이치몬", "二所ノ関一門");
		HeyaEntity ikazuchiHeya = heya("이카즈치", "雷", "데와노우미 이치몬", "出羽海一門");
		HeyaEntity onomatsuHeya = heya("오노마츠", "小野松", "토키츠카제 이치몬", "時津風一門"); // ⚠ 소속 이치몬 확인 필요
		
		
		// =====================================================
		// 2. 리키시 (기존 6명 + 신규 40명 = 46명)
		// =====================================================
		
		// --- 기존 6명 (프로필 유지, 상태만 갱신) ---
		
		// 2-1. 하쿠호 쇼: 오야카타직을 내려놓고 협회를 완전히 나감 (2025년 6월, 실제 사건 기반)
		//      → 더 이상 오야카타가 아니므로 currentRank/오야카타명/소속헤야를 모두 비움.
		//      → 선수 은퇴(2021) 사실 자체는 유지.
		RikishiEntity hakuho = RikishiEntity.builder()
				.externalApiId(1001)
				.shikonaKr("하쿠호 쇼")
				.shikonaJp("白鵬 翔")
				.name("Münkhbatyn Davaajargal")
				.birthdate(LocalDate.of(1985, 3, 11))
				.birthplace("몽골 울란바토르")
				.nationality("일본")
				.height(new BigDecimal("192.0"))
				.weight(new BigDecimal("155.0"))
				.heyaEntity(null) // 협회 탈퇴로 소속 없음
				.currentRank(null) // 오야카타도 현역도 아님
				.highestRank("Yokozuna")
				.isActive(false)
				.retiredDate(LocalDate.of(2021, 9, 30))
				.oyakataNameKr(null)
				.oyakataNameJp(null)
				.build();
		hakuho = rikishiRepository.save(hakuho);
		
		// 2-2. 테루노후지 하루오: 은퇴 후 "이세가하마" 명적을 계승해 이세가하마 오야카타가 됨
		RikishiEntity terunofuji = RikishiEntity.builder()
				.externalApiId(1002)
				.shikonaKr("테루노후지 하루오")
				.shikonaJp("照ノ富士 春雄")
				.name("Gantulgyn Gan-Erdene")
				.birthdate(LocalDate.of(1991, 11, 29))
				.birthplace("몽골 울란바토르")
				.nationality("일본")
				.height(new BigDecimal("192.0"))
				.weight(new BigDecimal("184.0"))
				.heyaEntity(isegahamaHeya)
				.currentRank("오야카타")
				.highestRank("Yokozuna")
				.isActive(false)
				.retiredDate(LocalDate.of(2025, 1, 13)) // 실제 은퇴 시점 추정치
				.oyakataNameKr("이세가하마")
				.oyakataNameJp("伊勢ヶ濱")
				.build();
		terunofuji = rikishiRepository.save(terunofuji);
		
		// 2-3. 아사노야마 히로키 (프로필 동일, 반즈케만 아래에서 마에가시라10으로 갱신)
		RikishiEntity asanoyama = RikishiEntity.builder()
				.externalApiId(1003)
				.shikonaKr("아사노야마 히로키")
				.shikonaJp("朝乃山 広暉")
				.name("Ishibashi Hiroki")
				.birthdate(LocalDate.of(1994, 3, 1))
				.birthplace("일본 토야마")
				.nationality("일본")
				.height(new BigDecimal("188.0"))
				.weight(new BigDecimal("175.0"))
				.heyaEntity(takasagoHeya)
				.currentRank("Maegashira")
				.highestRank("Ozeki")
				.isActive(true)
				.fightingStyle("요츠")
				.debutDate(LocalDate.of(2016, 3, 1))
				.photoUrl("/images/rikishi/asanoyama.webp")
				.build();
		asanoyama = rikishiRepository.save(asanoyama);
		
		// 2-4. 코토자쿠라 마사카츠 (2대, 프로필 동일, 반즈케는 서 오제키 그대로 유지)
		RikishiEntity kotozakura2nd = RikishiEntity.builder()
				.externalApiId(1004)
				.shikonaKr("코토자쿠라 마사카츠")
				.shikonaJp("琴櫻 将傑")
				.name("Kamatani Masakatsu")
				.birthdate(LocalDate.of(1997, 11, 19))
				.birthplace("일본 치바")
				.nationality("일본")
				.height(new BigDecimal("189.0"))
				.weight(new BigDecimal("178.0"))
				.heyaEntity(sadogatakeHeya)
				.currentRank("Ozeki")
				.highestRank("Ozeki")
				.isActive(true)
				.debutDate(LocalDate.of(2015, 11, 1))
				.build();
		kotozakura2nd = rikishiRepository.save(kotozakura2nd);
		
		// 2-5. 코토자쿠라 마사카츠 (1대, 할아버지, 은퇴/오야카타) - 변경 없음
		RikishiEntity kotozakura1st = RikishiEntity.builder()
				.externalApiId(1005)
				.shikonaKr("코토자쿠라 마사카츠")
				.shikonaJp("琴櫻 傑將")
				.name("Kamatani Norio")
				.birthdate(LocalDate.of(1940, 11, 26))
				.birthplace("일본 돗토리")
				.nationality("일본")
				.height(new BigDecimal("182.0"))
				.weight(new BigDecimal("150.0"))
				.heyaEntity(sadogatakeHeya)
				.currentRank("오야카타")
				.highestRank("Yokozuna")
				.isActive(false)
				.debutDate(LocalDate.of(1959, 1, 1))
				.retiredDate(LocalDate.of(1974, 7, 4))
				.oyakataNameKr("사도가타케")
				.oyakataNameJp("佐渡ヶ嶽")
				.build();
		kotozakura1st = rikishiRepository.save(kotozakura1st);
		
		// 2-6. 코토노와카 테루마사 (1대, 아버지, 은퇴/現 사도가타케 오야카타) - 변경 없음
		RikishiEntity kotonowaka1st = RikishiEntity.builder()
				.externalApiId(1006)
				.shikonaKr("코토노와카 테루마사")
				.shikonaJp("琴ノ若 晴將")
				.name("Konno Mitsuya")
				.birthdate(LocalDate.of(1968, 5, 15))
				.birthplace("일본 야마가타")
				.nationality("일본")
				.height(new BigDecimal("191.0"))
				.weight(new BigDecimal("181.0"))
				.heyaEntity(sadogatakeHeya)
				.currentRank("오야카타")
				.highestRank("Sekiwake")
				.isActive(false)
				.retiredDate(LocalDate.of(2005, 11, 26))
				.oyakataNameKr("사도가타케")
				.oyakataNameJp("佐渡ヶ嶽")
				.build();
		kotonowaka1st = rikishiRepository.save(kotonowaka1st);
		
		
		// --- 신규 40명 (2026년 7월 나고야바쇼 마쿠우치 반즈케 실명 반영) ---
		
		RikishiEntity hoshoryu = rikishi("호쇼류", "豊昇龍", "Byambasuren Battulga",
				LocalDate.of(1999, 5, 22), "몽골 울란바토르", "몽골", "187.0", "141.0",
				tatsunamiHeya, "Yokozuna", "Yokozuna");
		RikishiEntity onosato = rikishi("오노사토", "大の里", "Onosato Daiki",
				LocalDate.of(2000, 10, 25), "일본 이시카와", "일본", "192.0", "175.0",
				nishonosekiHeya, "Yokozuna", "Yokozuna");
		
		RikishiEntity kirishima = rikishi("키리시마", "霧島", "Bat-Erdene", // ⚠ 본명 추정
				LocalDate.of(1996, 8, 16), "몽골", "몽골", "179.0", "145.0",
				otowayamaHeya, "Ozeki", "Ozeki");
		
		RikishiEntity atamifuji = rikishi("아타미후지", "熱海富士", "Kawamata Ouzo", // ⚠ 본명 추정
				LocalDate.of(2003, 5, 12), "일본 시즈오카", "일본", "187.0", "162.0",
				isegahamaHeya, "Sekiwake", "Sekiwake");
		RikishiEntity kotoshoho = rikishi("코토쇼호", "琴勝峰", "Watanabe", // ⚠ 본명 추정
				LocalDate.of(2000, 1, 6), "일본 치바", "일본", "191.0", "158.0",
				sadogatakeHeya, "Sekiwake", "Sekiwake");
		RikishiEntity wakatakakage = rikishi("와카타카카게", "若隆景", "Kusano Yuya",
				LocalDate.of(1994, 6, 19), "일본 후쿠시마", "일본", "175.0", "128.0",
				arashioHeya, "Sekiwake", "Ozeki");
		RikishiEntity aonishiki = rikishi("아오니시키", "安青錦", "Danylo Yavhusishyn", // ⚠ 본명 추정
				LocalDate.of(2004, 3, 5), "우크라이나", "우크라이나", "185.0", "158.0",
				ajigawaHeya, "Sekiwake", "Sekiwake");
		
		RikishiEntity yoshinofuji = rikishi("요시노후지", "吉野富士", "Kimura", // ⚠ 시코나 한자/본명 추정
				LocalDate.of(1999, 2, 10), "일본 쿠마모토", "일본", "182.0", "150.0",
				isenoumiHeya, "Komusubi", "Komusubi");
		RikishiEntity oho = rikishi("오호", "大の湖", "Naya", // ⚠ 시코나 한자/본명 추정
				LocalDate.of(2000, 1, 6), "일본 도쿄", "일본", "192.0", "160.0",
				otakeHeya, "Komusubi", "Komusubi");
		
		RikishiEntity fujinokawa = rikishi("후지노카와", "富士乃川", "Yamamoto", // ⚠ 추정
				LocalDate.of(1997, 5, 1), "일본 교토", "일본", "183.0", "155.0",
				isenoumiHeya, "Maegashira", "Maegashira");
		RikishiEntity takanosho = rikishi("타카노쇼", "隆の勝", "Tomozawa Katsura", // ⚠ 본명 추정
				LocalDate.of(1994, 4, 19), "일본 치바", "일본", "183.0", "168.0",
				minatogawaHeya, "Maegashira", "Sekiwake");
		RikishiEntity gonoyama = rikishi("고노야마", "豪ノ山", "Kono", // ⚠ 시코나 한자/본명 추정
				LocalDate.of(1998, 9, 27), "일본 오사카", "일본", "186.0", "165.0",
				takekumaHeya, "Maegashira", "Maegashira");
		RikishiEntity churanoumi = rikishi("츄라노우미", "美ノ海", "Chinen", // ⚠ 시코나 한자/본명 추정
				LocalDate.of(1996, 11, 13), "일본 오키나와", "일본", "180.0", "148.0",
				kiseHeya, "Maegashira", "Maegashira");
		RikishiEntity hiradoumi = rikishi("히라도우미", "平戸海", "Kitamura Kaishu", // ⚠ 본명 추정
				LocalDate.of(1999, 1, 14), "일본 나가사키", "일본", "183.0", "160.0",
				sakaigawaHeya, "Maegashira", "Komusubi");
		RikishiEntity hakunofuji = rikishi("하쿠노후지", "白熊富士", "Nakashima", // ⚠ 시코나 한자/본명 추정
				LocalDate.of(1998, 6, 1), "일본 돗토리", "일본", "185.0", "155.0",
				isegahamaHeya, "Maegashira", "Maegashira");
		RikishiEntity daieisho = rikishi("다이에이쇼", "大栄翔", "Osada Yuya", // ⚠ 본명 추정
				LocalDate.of(1993, 4, 30), "일본 사이타마", "일본", "180.0", "168.0",
				oitekazeHeya, "Maegashira", "Sekiwake");
		RikishiEntity ichiyamamoto = rikishi("이치야마모토", "一山本", "Yamamoto Kohei", // ⚠ 본명 추정
				LocalDate.of(1994, 1, 25), "일본 홋카이도", "일본", "190.0", "175.0",
				hanaregomaHeya, "Maegashira", "Maegashira");
		RikishiEntity ura = rikishi("우라", "裏", "Kanetaka Ryoya", // ⚠ 본명 추정
				LocalDate.of(1993, 1, 22), "일본 오사카", "일본", "176.0", "139.0",
				kiseHeya, "Maegashira", "Maegashira");
		RikishiEntity oshoma = rikishi("오쇼마", "欧勝馬", "Enkhtaivan", // ⚠ 본명 추정
				LocalDate.of(1998, 5, 30), "몽골", "몽골", "178.0", "145.0",
				narutoHeya, "Maegashira", "Maegashira");
		RikishiEntity shodai = rikishi("쇼다이", "正代", "Nagano Masato",
				LocalDate.of(1991, 5, 14), "일본 쿠마모토", "일본", "181.0", "160.0",
				tokitsukazeHeya, "Maegashira", "Ozeki");
		RikishiEntity fujiseiun = rikishi("후지세이운", "富士靑雲", "Sakamoto", // ⚠ 시코나 한자/본명 추정
				LocalDate.of(1999, 10, 10), "일본 쿠마모토", "일본", "187.0", "150.0",
				fujishimaHeya, "Maegashira", "Maegashira");
		RikishiEntity kotoeiho = rikishi("코토에이호", "琴栄峰", "Yokoyama", // ⚠ 시코나 한자/본명 추정
				LocalDate.of(1998, 8, 8), "일본 치바", "일본", "193.0", "175.0",
				sadogatakeHeya, "Maegashira", "Maegashira");
		RikishiEntity takayasu = rikishi("타카야스", "高安", "Nakamura Takayasu",
				LocalDate.of(1990, 2, 27), "일본 이바라키", "일본", "187.0", "177.0",
				tagonouraHeya, "Maegashira", "Ozeki");
		RikishiEntity wakamotoharu = rikishi("와카모토하루", "若元春", "Kusano Motoharu",
				LocalDate.of(1992, 11, 14), "일본 후쿠시마", "일본", "179.0", "158.0",
				arashioHeya, "Maegashira", "Sekiwake");
		RikishiEntity roga = rikishi("로가", "露伊", "Roga", // ⚠ 시코나 한자/본명 추정
				LocalDate.of(1999, 2, 14), "러시아", "러시아", "190.0", "150.0",
				futagoyamaHeya, "Maegashira", "Maegashira");
		RikishiEntity fujiryoga = rikishi("후지료가", "富士龍", "Sato", // ⚠ 시코나 한자/본명 추정
				LocalDate.of(1999, 7, 7), "일본 아이치", "일본", "184.0", "155.0",
				fujishimaHeya, "Maegashira", "Maegashira");
		RikishiEntity tobizaru = rikishi("토비자루", "翔猿", "Nagatsuka Yuki",
				LocalDate.of(1993, 5, 8), "일본 도쿄", "일본", "168.0", "131.0",
				oitekazeHeya, "Maegashira", "Komusubi");
		RikishiEntity chiyoshoma = rikishi("치요쇼마", "千代翔馬", "Byambajav", // ⚠ 본명 추정
				LocalDate.of(1992, 6, 22), "몽골", "몽골", "180.0", "150.0",
				kokonoeHeya, "Maegashira", "Komusubi");
		RikishiEntity wakanosho = rikishi("와카노쇼", "若ノ城", "Suzuki", // ⚠ 시코나 한자/본명 추정
				LocalDate.of(1997, 5, 19), "일본 토치기", "일본", "187.0", "165.0",
				minatogawaHeya, "Maegashira", "Maegashira");
		RikishiEntity mitakeumi = rikishi("미타케우미", "御嶽海", "Ozeki Miki",
				LocalDate.of(1992, 2, 19), "일본 나가노", "일본", "177.0", "175.0",
				dewanoumiHeya, "Maegashira", "Ozeki");
		RikishiEntity asahakuryu = rikishi("아사하쿠류", "朝白龍", "Ganbold", // ⚠ 본명 추정
				LocalDate.of(1994, 11, 11), "몽골", "몽골", "179.0", "140.0",
				takasagoHeya, "Maegashira", "Maegashira", LocalDate.of(2023, 1, 1));
		RikishiEntity abi = rikishi("아비", "阿炎", "Abe Kazuma", // ⚠ 본명 추정
				LocalDate.of(1993, 9, 24), "일본 사이타마", "일본", "189.0", "155.0",
				shikoroyamaHeya, "Maegashira", "Komusubi");
		RikishiEntity nishikifuji = rikishi("니시키후지", "錦富士", "Kikuchi", // ⚠ 본명 추정
				LocalDate.of(1994, 10, 24), "일본 아오모리", "일본", "180.0", "160.0",
				isegahamaHeya, "Maegashira", "Maegashira");
		RikishiEntity takerufuji = rikishi("타케루후지", "尊富士", "Kobayashi Ouga", // ⚠ 본명 추정
				LocalDate.of(1999, 1, 5), "일본 아오모리", "일본", "180.0", "160.0",
				isegahamaHeya, "Maegashira", "Maegashira");
		RikishiEntity kinbozan = rikishi("킨보잔", "金峰山", "Kinbozan Erlan", // ⚠ 본명 추정
				LocalDate.of(1996, 10, 6), "카자흐스탄", "카자흐스탄", "191.0", "175.0",
				kiseHeya, "Maegashira", "Maegashira");
		RikishiEntity shishi = rikishi("시시", "獅司", "Shishi", // ⚠ 시코나 한자/본명 추정
				LocalDate.of(1998, 3, 3), "우크라이나", "우크라이나", "183.0", "150.0",
				ikazuchiHeya, "Maegashira", "Maegashira");
		RikishiEntity onokatsu = rikishi("오노카츠", "翁鵬", "Onokatsu", // ⚠ 시코나 한자/본명 추정
				LocalDate.of(1997, 4, 4), "몽골", "몽골", "178.0", "140.0",
				onomatsuHeya, "Maegashira", "Maegashira");
		RikishiEntity kazuma = rikishi("카즈마", "一磨", "Kazuma", // ⚠ 시코나 한자/본명 추정
				LocalDate.of(1992, 9, 9), "일본 오사카", "일본", "178.0", "150.0",
				kiseHeya, "Maegashira", "Maegashira");
		RikishiEntity daiseizan = rikishi("다이세이잔", "大青山", "Daiseizan", // ⚠ 시코나 한자/본명 추정
				LocalDate.of(1997, 12, 12), "중국", "중국", "193.0", "175.0",
				arashioHeya, "Maegashira", "Maegashira");
		RikishiEntity asakoryu = rikishi("아사코류", "朝紅龍", "Asakoryu", // ⚠ 시코나 한자/본명 추정
				LocalDate.of(2002, 5, 5), "일본 오사카", "일본", "178.0", "140.0",
				takasagoHeya, "Maegashira", "Maegashira", LocalDate.of(2021, 5, 1));
		
		
		// =====================================================
		// 3. 헤야 대표 오야카타 지정 (순환참조 해제 - ALTER 대응)
		// =====================================================
//		miyaginoHeya.updateMasterRikishi(null); // 폐쇄로 대표 오야카타 없음
		isegahamaHeya.updateMasterRikishi(terunofuji);
		sadogatakeHeya.updateMasterRikishi(kotonowaka1st);
		
		
		// =====================================================
		// 4. 시코나 변경 이력 (기존 2건 유지)
		// =====================================================
		RikishiShikonaHistoryEntity asanoyamaHistory = RikishiShikonaHistoryEntity.builder()
				.rikishiEntity(asanoyama)
				.shikonaKr("이시바시")
				.shikonaJp("石橋")
				.validFrom(LocalDate.of(2015, 11, 1))
				.validTo(LocalDate.of(2016, 11, 30))
				.build();
		rikishiShikonaHistoryRepository.save(asanoyamaHistory);
		
		RikishiShikonaHistoryEntity kotozakuraHistory1 = RikishiShikonaHistoryEntity.builder()
				.rikishiEntity(kotozakura2nd)
				.shikonaKr("코토카마타니")
				.shikonaJp("琴鎌谷")
				.validFrom(LocalDate.of(2017, 3, 1))
				.validTo(LocalDate.of(2022, 10, 31))
				.build();
		rikishiShikonaHistoryRepository.save(kotozakuraHistory1);
		
		RikishiShikonaHistoryEntity kotozakuraHistory2 = RikishiShikonaHistoryEntity.builder()
				.rikishiEntity(kotozakura2nd)
				.shikonaKr("코토노와카")
				.shikonaJp("琴ノ若")
				.validFrom(LocalDate.of(2022, 11, 1))
				.validTo(LocalDate.of(2024, 1, 31))
				.build();
		rikishiShikonaHistoryRepository.save(kotozakuraHistory2);
		
		
		// =====================================================
		// 5. 바쇼 (2026년 7월 나고야바쇼)
		// =====================================================
		BashoEntity bashoJul2026 = BashoEntity.builder()
				.bashoYear(2026)
				.bashoMonth(BashoMonth.JUL)
				.startDate(LocalDate.of(2026, 7, 12))
				.endDate(LocalDate.of(2026, 7, 26))
				.build();
		bashoJul2026 = bashoRepository.save(bashoJul2026);
		
		
		// =====================================================
		// 6. 반즈케 - 2026년 7월 나고야바쇼 마쿠우치 36명 실제 반즈케
		//    ※ 세키와케 순번은 원본 자료상 "서 세키와케2 코토쇼호"로 보이나, 세키와케2 자리에
		//      동/서가 각 1명씩만 존재해야 하므로 "서 세키와케1 코토쇼호"의 오기로 보고 보정했습니다.
		// =====================================================
		banzuke(hoshoryu, bashoJul2026, RankName.Yokozuna, Side.EAST, null);
		banzuke(onosato, bashoJul2026, RankName.Yokozuna, Side.WEST, null);
		
		banzuke(kirishima, bashoJul2026, RankName.Ozeki, Side.EAST, 1);
		banzuke(kotozakura2nd, bashoJul2026, RankName.Ozeki, Side.WEST, 1);
		
		banzuke(atamifuji, bashoJul2026, RankName.Sekiwake, Side.EAST, 1);
		banzuke(kotoshoho, bashoJul2026, RankName.Sekiwake, Side.WEST, 1); // ※ 위 보정 참고
		banzuke(wakatakakage, bashoJul2026, RankName.Sekiwake, Side.EAST, 2);
		banzuke(aonishiki, bashoJul2026, RankName.Sekiwake, Side.WEST, 2);
		
		banzuke(yoshinofuji, bashoJul2026, RankName.Komusubi, Side.EAST, 1);
		banzuke(oho, bashoJul2026, RankName.Komusubi, Side.WEST, 1);
		
		banzuke(fujinokawa, bashoJul2026, RankName.Maegashira, Side.EAST, 1);
		banzuke(takanosho, bashoJul2026, RankName.Maegashira, Side.WEST, 1);
		banzuke(gonoyama, bashoJul2026, RankName.Maegashira, Side.EAST, 2);
		banzuke(churanoumi, bashoJul2026, RankName.Maegashira, Side.WEST, 2);
		banzuke(hiradoumi, bashoJul2026, RankName.Maegashira, Side.EAST, 3);
		banzuke(hakunofuji, bashoJul2026, RankName.Maegashira, Side.WEST, 3);
		banzuke(daieisho, bashoJul2026, RankName.Maegashira, Side.EAST, 4);
		banzuke(ichiyamamoto, bashoJul2026, RankName.Maegashira, Side.WEST, 4);
		banzuke(ura, bashoJul2026, RankName.Maegashira, Side.EAST, 5);
		banzuke(oshoma, bashoJul2026, RankName.Maegashira, Side.WEST, 5);
		banzuke(shodai, bashoJul2026, RankName.Maegashira, Side.EAST, 6);
		banzuke(fujiseiun, bashoJul2026, RankName.Maegashira, Side.WEST, 6);
		banzuke(kotoeiho, bashoJul2026, RankName.Maegashira, Side.EAST, 7);
		banzuke(takayasu, bashoJul2026, RankName.Maegashira, Side.WEST, 7);
		banzuke(wakamotoharu, bashoJul2026, RankName.Maegashira, Side.EAST, 8);
		banzuke(roga, bashoJul2026, RankName.Maegashira, Side.WEST, 8);
		banzuke(fujiryoga, bashoJul2026, RankName.Maegashira, Side.EAST, 9);
		banzuke(tobizaru, bashoJul2026, RankName.Maegashira, Side.WEST, 9);
		banzuke(asanoyama, bashoJul2026, RankName.Maegashira, Side.EAST, 10);
		banzuke(chiyoshoma, bashoJul2026, RankName.Maegashira, Side.WEST, 10);
		banzuke(wakanosho, bashoJul2026, RankName.Maegashira, Side.EAST, 11);
		banzuke(mitakeumi, bashoJul2026, RankName.Maegashira, Side.WEST, 11);
		banzuke(asahakuryu, bashoJul2026, RankName.Maegashira, Side.EAST, 12);
		banzuke(abi, bashoJul2026, RankName.Maegashira, Side.WEST, 12);
		banzuke(nishikifuji, bashoJul2026, RankName.Maegashira, Side.EAST, 13);
		banzuke(takerufuji, bashoJul2026, RankName.Maegashira, Side.WEST, 13);
		banzuke(kinbozan, bashoJul2026, RankName.Maegashira, Side.EAST, 14);
		banzuke(shishi, bashoJul2026, RankName.Maegashira, Side.WEST, 14);
		banzuke(onokatsu, bashoJul2026, RankName.Maegashira, Side.EAST, 15);
		banzuke(kazuma, bashoJul2026, RankName.Maegashira, Side.WEST, 15);
		banzuke(daiseizan, bashoJul2026, RankName.Maegashira, Side.EAST, 16);
		banzuke(asakoryu, bashoJul2026, RankName.Maegashira, Side.WEST, 16);
		
		// 하쿠호/테루노후지는 이번 바쇼(2026.07) 시점에 이미 협회를 떠났거나 오야카타이므로
		// 반즈케 데이터 없음 (은퇴 전 하쿠호 vs 테루노후지 더미 대전도 함께 제거함)
		
		// =====================================================
		// 7. 토리쿠미(15일치 대전) - 현역 3명(아사노야마/아사코류/아사하쿠류) 호시토리표 시연용 더미데이터
		//    - 상대역 19명 고정 순번 풀에서 (day + offset) % pool.size() 로 1차 상대를 정하고,
		//      ① 본인 자신 ② 그날 이미 다른 상대와 배정된 경우 ③ 같은 메인에게 이미 배정됐던 상대
		//      중 하나라도 걸리면 다음 인덱스로 순차 보정합니다.
		//      ⚠ 아사코류/아사하쿠류는 19명 풀에도 서로 포함돼 있어서, 노트에 적힌 "%19" 공식을
		//        그대로만 적용하면 (a) 특정 날짜에 자기 자신과 맞붙거나 (b) 세 메인 중 두 명이
		//        같은 날 서로를 상대로 뽑히면서 그중 한쪽이 같은 날 경기가 2개가 되는 문제가
		//        실제로 발생했습니다 (예: 원래 로직대로면 10일차에 아사코류→아사하쿠류로 배정되는데,
		//        아사하쿠류 본인 쪽 계산도 별개로 10일차에 다른 상대가 나와서 이중 출전이 됨).
		//        그래서 같은 날 두 메인이 서로를 상대로 뽑히면 그 경기 1건만 저장하고, 나중에 계산되는
		//        쪽은 "이미 그날 배정된 상대"를 그대로 이어받아 새 경기를 만들지 않도록 했습니다.
		//        기본 공식(offset 0/7/13)은 노트 그대로 유지했습니다.
		//    - 계급(반즈케)은 이 표에서 별도로 저장하지 않고, 동서 배정/승패/결정기술만 임의 생성합니다.
		// =====================================================
		List<RikishiEntity> torikumiOpponentPool = List.of(
				hoshoryu, onosato, kirishima, aonishiki, wakatakakage, atamifuji, kotoshoho,
				yoshinofuji, oho, fujinokawa, takanosho, fujiryoga, hiradoumi, churanoumi,
				ichiyamamoto, daieisho, ura, asakoryu, asahakuryu
		);
		// day -> (rikishiId -> 그날 상대) : 메인 3명 간 겹치는 날짜의 매치를 한 건으로 공유하기 위한 기록
		Map<Integer, Map<Integer, RikishiEntity>> torikumiAssignmentByDay = new HashMap<>();
		seedNagoya2026Torikumi(bashoJul2026, torikumiOpponentPool, asanoyama, 0, torikumiAssignmentByDay);
		seedNagoya2026Torikumi(bashoJul2026, torikumiOpponentPool, asakoryu, 7, torikumiAssignmentByDay);
		seedNagoya2026Torikumi(bashoJul2026, torikumiOpponentPool, asahakuryu, 13, torikumiAssignmentByDay);
		
		System.out.println("====== [DataSeeder] 2026 나고야바쇼 기준 데이터 생성 완료! ======");
	}
	
	
	// =====================================================
	// 헬퍼 메서드
	// =====================================================
	
	private HeyaEntity heya(String nameKr, String nameJp, String ichimonKr, String ichimonJp) {
		return heyaRepository.save(HeyaEntity.builder()
				.nameKr(nameKr)
				.nameJp(nameJp)
				.ichimonKr(ichimonKr)
				.ichimonJp(ichimonJp)
				.build());
	}
	
	private RikishiEntity rikishi(String shikonaKr, String shikonaJp, String name, LocalDate birthdate,
								  String birthplace, String nationality, String heightCm, String weightKg,
								  HeyaEntity heya, String currentRank, String highestRank) {
		return rikishi(shikonaKr, shikonaJp, name, birthdate, birthplace, nationality, heightCm, weightKg,
				heya, currentRank, highestRank, null, null);
	}
	
	// 데뷔일(debutDate)이 필요한 경우 사용 (예: 아사코류, 아사하쿠류)
	private RikishiEntity rikishi(String shikonaKr, String shikonaJp, String name, LocalDate birthdate,
								  String birthplace, String nationality, String heightCm, String weightKg,
								  HeyaEntity heya, String currentRank, String highestRank, LocalDate debutDate) {
		return rikishi(shikonaKr, shikonaJp, name, birthdate, birthplace, nationality, heightCm, weightKg,
				heya, currentRank, highestRank, debutDate, null);
	}
	
	// 사진(photoUrl)까지 필요한 경우 사용
	private RikishiEntity rikishi(String shikonaKr, String shikonaJp, String name, LocalDate birthdate,
								  String birthplace, String nationality, String heightCm, String weightKg,
								  HeyaEntity heya, String currentRank, String highestRank,
								  LocalDate debutDate, String photoUrl) {
		return rikishiRepository.save(RikishiEntity.builder()
				.externalApiId(nextExternalId++)
				.shikonaKr(shikonaKr)
				.shikonaJp(shikonaJp)
				.name(name)
				.birthdate(birthdate)
				.birthplace(birthplace)
				.nationality(nationality)
				.height(new BigDecimal(heightCm))
				.weight(new BigDecimal(weightKg))
				.heyaEntity(heya)
				.currentRank(currentRank)
				.highestRank(highestRank)
				.debutDate(debutDate)
				.photoUrl(photoUrl)
				.isActive(true)
				.build());
	}
	
	private void banzuke(RikishiEntity rikishi, BashoEntity basho, RankName rankName, Side side, Integer rankValue) {
		banzukeRepository.save(BanzukeEntity.builder()
				.rikishiEntity(rikishi)
				.bashoEntity(basho)
				.division(Division.Makuuchi)
				.rankName(rankName)
				.side(side)
				.rankValue(rankValue)
				.build());
	}
	
	/**
	 * 메인 리키시 1명의 15일치 토리쿠미(대전) 더미데이터를 생성한다.
	 * 아사노야마 → 아사코류 → 아사하쿠류 순으로 호출되며, assignmentByDay를 공유해서
	 * 뒤에 처리되는 메인이 앞에서 이미 배정된 날짜/상대와 겹치지 않도록 조율한다.
	 *
	 * @param basho            바쇼
	 * @param opponentPool     상대역 후보 풀 (고정 순번)
	 * @param mainRikishi      호시토리표 대상 리키시 (아사노야마/아사코류/아사하쿠류)
	 * @param offset           (day + offset) % pool.size() 계산에 쓰이는 개인별 오프셋
	 * @param assignmentByDay  day -> (rikishiId -> 그날 배정된 상대). 호출부에서 3명이 공유하는 상태.
	 *                         이미 이 메인이 그날 다른 메인의 상대로 배정되어 있으면 새 경기를 만들지 않고
	 *                         그 배정을 그대로 이어받는다 (같은 경기가 두 건으로 중복 저장되는 것을 방지).
	 */
	private void seedNagoya2026Torikumi(BashoEntity basho, List<RikishiEntity> opponentPool,
										RikishiEntity mainRikishi, int offset,
										Map<Integer, Map<Integer, RikishiEntity>> assignmentByDay) {
		int poolSize = opponentPool.size();
		Set<Integer> usedOpponentIds = new HashSet<>(); // 이 메인 리키시가 이미 대전한 상대 (15일 내 중복 상대 방지)
		
		for (int day = 1; day <= 15; day++) {
			Map<Integer, RikishiEntity> dayAssignment = assignmentByDay.computeIfAbsent(day, d -> new HashMap<>());
			
			if (dayAssignment.containsKey(mainRikishi.getId())) {
				// 앞서 처리된 다른 메인의 상대로 이미 이날 경기가 배정됨 - 새로 만들지 않고 건너뜀
				continue;
			}
			
			int idx = (day + offset) % poolSize;
			RikishiEntity opponent = opponentPool.get(idx);
			int probes = 0;
			while ((opponent.getId().equals(mainRikishi.getId())
					|| dayAssignment.containsKey(opponent.getId())
					|| usedOpponentIds.contains(opponent.getId()))
					&& probes < poolSize) {
				idx = (idx + 1) % poolSize;
				opponent = opponentPool.get(idx);
				probes++;
			}
			
			usedOpponentIds.add(opponent.getId());
			dayAssignment.put(mainRikishi.getId(), opponent);
			dayAssignment.put(opponent.getId(), mainRikishi);
			
			boolean mainIsEast = random.nextBoolean();
			RikishiEntity east = mainIsEast ? mainRikishi : opponent;
			RikishiEntity west = mainIsEast ? opponent : mainRikishi;
			
			boolean mainWins = random.nextBoolean();
			RikishiEntity winner = mainWins ? mainRikishi : opponent;
			RikishiEntity loser = mainWins ? opponent : mainRikishi;
			String kimarite = KIMARITE_POOL.get(random.nextInt(KIMARITE_POOL.size()));

			// 토리쿠미 상세 페이지에서 보여줄 간단한 해설 더미. 유튜브 URL은 실제 영상이 없어 비워 둔다
			// (상세 페이지는 영상이 없으면 "등록된 하이라이트 영상이 없습니다"로 처리).
			String winnerShikona = winner.getShikonaKr() != null ? winner.getShikonaKr().split(" ")[0] : "승자";
			String loserShikona = loser.getShikonaKr() != null ? loser.getShikonaKr().split(" ")[0] : "패자";
			String descriptionKr = day + "일째, " + winnerShikona + "이(가) " + kimarite + "(으)로 " + loserShikona + "을(를) 꺾었다.";

			torikumiRepository.save(TorikumiEntity.builder()
					.bashoEntity(basho)
					.day(day)
					.division(Division.Makuuchi)
					.eastRikishiEntity(east)
					.westRikishiEntity(west)
					.winnerRikishiEntity(winner)
					.loserRikishiEntity(loser)
					.resultType(ResultType.NORMAL)
					.kimarite(kimarite)
					.isExtraMatch(false)
					.descriptionKr(descriptionKr)
					.build());
		}
	}
}