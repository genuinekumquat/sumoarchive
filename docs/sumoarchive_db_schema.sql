-- =====================================================
-- 스모아카이브 DB 스키마
-- =====================================================

use sumo;
-- =====================================================
-- SECTION 1. 헤야 & 리키시
-- =====================================================

-- 1-1. 헤야 테이블
-- heyaEntity, rikishiEntity 간 순환 참조로 인해 master_rikishi_id FK는 1-3에서 추가
CREATE TABLE heyaEntity (
    id               INT          AUTO_INCREMENT PRIMARY KEY,
    name_kr          VARCHAR(100) NOT NULL,       -- 헤야 이름 (한국어)
    name_jp          VARCHAR(100) NOT NULL,       -- 헤야 이름 (일본어)
    ichimon_kr       VARCHAR(100),                -- 소속 이치몬 (한국어)
    ichimon_jp       VARCHAR(100),                -- 소속 이치몬 (일본어)
    master_rikishi_id INT,                        -- 헤야 대표 오야카타 ID (FK는 하단 ALTER로 추가)
    created_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- 1-2. 리키시 테이블 (현역 선수 + 은퇴 오야카타 통합)
CREATE TABLE rikishiEntity (
    id               INT           AUTO_INCREMENT PRIMARY KEY,
    external_api_id  INT UNIQUE,                  -- sumo-api 선수 고유 ID (동기화용)
    shikona_kr       VARCHAR(100),                -- 시코나 (한국어)
    shikona_jp       VARCHAR(100),                -- 시코나 (일본어)
    name             VARCHAR(100),                -- 본명
    birthdate        DATE,                        -- 생년월일
    birthplace       VARCHAR(255),                -- 출생지
    nationality      VARCHAR(100),                -- 국적 ('일본', '몽골', '조지아' 등 한국어 통일)
    height           DECIMAL(4,1),                -- 키 (cm)
    weight           DECIMAL(4,1),                -- 몸무게 (kg)
    heya_id          INT,                         -- 소속 헤야 ID
    current_rank     VARCHAR(50),                 -- 현재 계급 (캐시용 / source of truth는 banzuke)
    highest_rank     VARCHAR(50),                 -- 최고 계급
    fighting_style   VARCHAR(255),                -- 파이팅 스타일 (관리자 입력)
    debut_date       DATE,                        -- 데뷔일
    is_active        BOOLEAN NOT NULL DEFAULT TRUE,  -- 현역 여부 (0 = 은퇴 / 오야카타 포함)
    retired_date     DATE,                        -- 은퇴일 (현역이면 NULL)
    oyakata_name_kr  VARCHAR(100),                -- 은퇴 후 오야카타 습명 (한국어, 현역이면 NULL)
    oyakata_name_jp  VARCHAR(100),                -- 은퇴 후 오야카타 습명 (일본어, 현역이면 NULL)
    photo_url        VARCHAR(255),                -- 프로필 사진 URL
    created_at       TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (heya_id) REFERENCES heyaEntity(id) ON DELETE SET NULL
);

-- 1-3. 헤야 ↔ 리키시 순환 참조 해결: 리키시 테이블 생성 후 FK 추가
ALTER TABLE heyaEntity
    ADD CONSTRAINT fk_heya_master_rikishi
    FOREIGN KEY (master_rikishi_id) REFERENCES rikishiEntity(id) ON DELETE SET NULL;

-- 1-4. 리키시 시코나 변경 이력 테이블
CREATE TABLE rikishi_shikona_history (
    id          INT          AUTO_INCREMENT PRIMARY KEY,
    rikishi_id  INT          NOT NULL,            -- 대상 선수 ID
    shikona_kr  VARCHAR(100) NOT NULL,            -- 과거 시코나 (한국어)
    shikona_jp  VARCHAR(100) NOT NULL,            -- 과거 시코나 (일본어)
    valid_from  DATE,                             -- 해당 시코나 사용 시작일
    valid_to    DATE,                             -- 해당 시코나 사용 종료일 (NULL = 현재 사용 중)
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (rikishi_id) REFERENCES rikishiEntity(id) ON DELETE CASCADE
);


-- =====================================================
-- SECTION 2. 바쇼 & 반즈케
-- =====================================================

-- 2-1. 바쇼(대회) 마스터 테이블
CREATE TABLE bashoEntity (
    id           INT  AUTO_INCREMENT PRIMARY KEY,
    basho_year   INT  NOT NULL,
    basho_month  ENUM('JAN','MAR','MAY','JUL','SEP','NOV') NOT NULL,  -- 연 6회 고정 개최월
    start_date   DATE,
    end_date     DATE,

    UNIQUE KEY uq_basho (basho_year, basho_month)
);

-- 2-2. 반즈케 테이블 (바쇼별 선수 계급 기록)
CREATE TABLE banzuke (
    id          INT  AUTO_INCREMENT PRIMARY KEY,
    rikishi_id  INT  NOT NULL,
    basho_id    INT  NOT NULL,

    division    ENUM('Makuuchi','Juryo','Makushita','Sandanme','Jonidan','Jonokuchi') NOT NULL,
    rank_name        ENUM('Yokozuna','Ozeki','Sekiwake','Komusubi','Maegashira',
                     'Juryo','Makushita','Sandanme','Jonidan','Jonokuchi')           NOT NULL,
    side        ENUM('EAST','WEST')  NOT NULL,
    rank_value  INT,                              -- 마에가시라 등 숫자 서열 (요코즈나 등은 NULL)
    rank_score  INT,                              -- 전체 서열 점수 (변화량 계산용, 추후 채워넣기)

    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uq_rikishi_basho (rikishi_id, basho_id),  -- 한 바쇼에 한 선수의 반즈케는 하나
    FOREIGN KEY (rikishi_id) REFERENCES rikishiEntity(id) ON DELETE CASCADE,
    FOREIGN KEY (basho_id)   REFERENCES bashoEntity(id)   ON DELETE CASCADE
);


-- =====================================================
-- SECTION 3. 토리쿠미 (경기 결과)
-- =====================================================

CREATE TABLE torikumiEntity (
    id                INT      AUTO_INCREMENT PRIMARY KEY,
    basho_id          INT      NOT NULL,
    day               INT      NOT NULL,          -- 경기 일차 (1~15, 결정전은 16 이상)
    division          ENUM('Makuuchi','Juryo','Makushita','Sandanme','Jonidan','Jonokuchi') NOT NULL,
    match_no    	  INT,                        -- 당일 경기 순서 (1, 2, 3...)
	external_id       VARCHAR(50) UNIQUE,         -- API의 torikumiEntity id (중복 적재 방지용)

    east_rikishi_id   INT      NOT NULL,          -- 동쪽 선수
    west_rikishi_id   INT      NOT NULL,          -- 서쪽 선수
    winner_rikishi_id INT,                        -- 승자 ID (부전패 등 예외 케이스 대비 NULL 허용)
    loser_rikishi_id  INT,                        -- 패자 ID (빠른 조회용)

    result_type       ENUM('NORMAL','FUZEN') NOT NULL DEFAULT 'NORMAL',  -- 정상 경기 / 부전승패 구분
    kimarite          VARCHAR(100),               -- 결정 기술 (예: 'Yorikiri', 부전패면 NULL)
    is_extra_match    BOOLEAN NOT NULL DEFAULT FALSE,     -- 동점 결정전 여부

    youtube_url       VARCHAR(255),               -- 하이라이트 유튜브 링크 (관리자 입력)
    description_kr    TEXT,                       -- 경기 설명 (한국어, 관리자 입력)
    description_jp    TEXT,                       -- 경기 설명 (일본어, 관리자 입력)

    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uq_torikumi (basho_id, day, east_rikishi_id, west_rikishi_id, is_extra_match),
    FOREIGN KEY (basho_id)          REFERENCES bashoEntity(id)    ON DELETE CASCADE,
    FOREIGN KEY (east_rikishi_id)   REFERENCES rikishiEntity(id),
    FOREIGN KEY (west_rikishi_id)   REFERENCES rikishiEntity(id),
    FOREIGN KEY (winner_rikishi_id) REFERENCES rikishiEntity(id),
    FOREIGN KEY (loser_rikishi_id)  REFERENCES rikishiEntity(id)
);


-- =====================================================
-- SECTION 4. 수상 경력
-- =====================================================

-- 4-1. 수상 테이블 (유쇼 & 삼상)
CREATE TABLE award (
    id          INT  AUTO_INCREMENT PRIMARY KEY,
    rikishi_id  INT  NOT NULL,
    basho_id    INT  NOT NULL,
    division    ENUM('Makuuchi','Juryo','Makushita','Sandanme','Jonidan','Jonokuchi') NOT NULL,
    award_type  ENUM('YUSHO','SANSHO_SHUKUN','SANSHO_KANTO','SANSHO_GINO')          NOT NULL,

    UNIQUE KEY uq_award (rikishi_id, basho_id, division, award_type),  -- 동일 수상 중복 방지
    FOREIGN KEY (rikishi_id) REFERENCES rikishiEntity(id),
    FOREIGN KEY (basho_id)   REFERENCES bashoEntity(id)
);

-- 4-2. 킨보시 테이블 (마에가시라의 요코즈나 격파 기록)
CREATE TABLE kinboshi (
    id                INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    torikumi_id       INT NOT NULL,               -- 킨보시가 발생한 경기
    winner_rikishi_id INT NOT NULL,               -- 이긴 마에가시라
    loser_yokozuna_id INT NOT NULL,               -- 패배한 요코즈나

    FOREIGN KEY (torikumi_id)       REFERENCES torikumiEntity(id) ON DELETE CASCADE,
    FOREIGN KEY (winner_rikishi_id) REFERENCES rikishiEntity(id),
    FOREIGN KEY (loser_yokozuna_id) REFERENCES rikishiEntity(id)
);


-- =====================================================
-- SECTION 5. 댓글
-- =====================================================

CREATE TABLE comment (
    id           INT          AUTO_INCREMENT PRIMARY KEY,
    torikumi_id  INT          NOT NULL,
    nickname     VARCHAR(8)   NOT NULL,           -- 닉네임 (최대 8자)
    password     VARCHAR(255) NOT NULL,           -- 비밀번호 (BCrypt 암호화 저장)
    content      VARCHAR(200) NOT NULL,           -- 댓글 내용 (최대 200자)
    is_deleted   BOOLEAN      DEFAULT FALSE,      -- 관리자 가림처리용 (소프트 딜리트)
    created_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (torikumi_id) REFERENCES torikumiEntity(id) ON DELETE CASCADE
);


-- =====================================================
-- 관리자 계정은 DB 테이블 없이 application.properties에 하드코딩
-- (MVP 기준 / 추후 admin 테이블로 분리 가능)
-- =====================================================
