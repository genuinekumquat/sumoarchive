# SUMOARCHIVE 운영 서버 배포 가이드 (Production Deployment Guide)

이 문서는 **SUMOARCHIVE**를 실제 운영 서버(AWS, GCP, Oracle Cloud, VPS 등)에 배포하고 운영하기 위한 실전 가이드입니다.

---

## 1. 아키텍처 개요

```
[사용자 브라우저]
       │ (HTTPS : 443 / HTTP : 80)
       ▼
[Nginx 리버스 프록시]
  - SSL/TLS (Let's Encrypt)
  - Gzip 압축 & 캐싱 헤더
  - no-referrer 및 보안 헤더
       │ (Proxy Pass : 8080)
       ▼
[Spring Boot 애플리케이션 (Docker)]
  - Java 21 JRE (Eclipse Temurin Alpine)
  - Spring Profile: prod
  - 인메모리 Rate Limiter & Spring Cache (Banzuke, Kimarite)
       │ (JDBC : 3306)
       ▼
[MySQL 8.0 데이터베이스]
```

---

## 2. 배포 사전 준비

### 1) 환경변수 파일 (`.env`) 생성
프로젝트 루트의 `.env.example`을 복사하여 실제 운영 정보를 입력합니다.

```bash
cp .env.example .env
nano .env
```

**.env 예시**:
```properties
# 1. 활성 프로필 및 포트
SPRING_PROFILES_ACTIVE=prod
PORT=8080

# 2. MySQL 접속 정보
SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/sumo?useSSL=false&characterEncoding=UTF-8&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true
SPRING_DATASOURCE_USERNAME=sumo_prod_user
SPRING_DATASOURCE_PASSWORD=강력한_DB_비밀번호_입력!
MYSQL_ROOT_PASSWORD=강력한_ROOT_비밀번호_입력!

# 3. 어드민 계정
ADMIN_USERNAME=sumoAdmin
ADMIN_PASSWORD=강력한_관리자_비밀번호_입력!

# 4. 개인정보 처리 안내(/privacy)에 공개할 문의 이메일 (없으면 앱이 시작하지 않음)
CONTACT_EMAIL=문의용_이메일@example.com
```

---

## 3. 배포 실행 방법

### 방법 A: Docker Compose를 이용한 원클릭 배포 (권장)

서버에 Docker와 Docker Compose가 설치되어 있다면 가장 간단하고 안정적인 방법입니다.

1. **컨테이너 빌드 및 백그라운드 실행**:
   ```bash
   docker compose up -d --build
   ```

2. **로그 확인**:
   ```bash
   docker compose logs -f app                     # 최근 콘솔 출력 (컨테이너당 10MB x 5개까지만 보관)
   docker compose exec app ls -l /app/logs        # 앱 로그 파일 목록
   docker compose exec app tail -n 200 /app/logs/sumoarchive.log
   docker compose exec app zcat /app/logs/sumoarchive.log.2026-09-30.0.gz | grep ERROR   # 지난 로그 검색
   ```
   - 앱 로그 파일은 `sumo_app_logs` 볼륨에 쌓여서 `docker compose down`·재빌드 후에도 남는다.
   - 하루 단위·10MB 단위로 나뉘어 gzip 압축되고, 14일이 지나거나 합계 500MB를 넘으면 오래된 것부터 지워진다 (`application-prod.properties`).
   - `docker compose down -v`는 DB와 함께 로그 볼륨도 지우므로 쓰지 않는다.

3. **컨테이너 중지 / 재시작**:
   ```bash
   docker compose down
   docker compose restart app
   ```

4. **상태 확인 (헬스체크)**:
   ```bash
   docker compose ps                          # app이 (healthy)로 표시되면 정상
   curl http://127.0.0.1:8080/actuator/health # {"status":"UP"} - DB 연결까지 확인
   ```
   외부 업타임 모니터(UptimeRobot 등)에는 `https://도메인/actuator/health`를 등록해 두면, 앱이나 DB가 죽었을 때 알림을 받을 수 있다.
   노출되는 actuator 엔드포인트는 `health` 하나뿐이고 세부 정보는 표시하지 않는다.

---

### 방법 B: 단일 JAR 파일 직접 실행 (Standalone)

서버에 Java 21이 이미 설치되어 있고 외부 관리형 DB(AWS RDS 등)를 사용할 때 적합합니다.

1. **빌드 (JAR 생성)**:
   ```bash
   ./gradlew bootJar -x test
   ```

2. **환경변수와 함께 실행**:
   ```bash
   export SPRING_PROFILES_ACTIVE=prod
   export SPRING_DATASOURCE_URL="jdbc:mysql://<DB_HOST>:3306/sumo?useSSL=false&characterEncoding=UTF-8&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true"
   export SPRING_DATASOURCE_USERNAME="sumo_prod_user"
   export SPRING_DATASOURCE_PASSWORD="db_password"
   export ADMIN_PASSWORD="admin_password"

   java -jar -Dfile.encoding=UTF-8 -XX:+UseG1GC -XX:MaxRAMPercentage=75.0 build/libs/sumoarchive-0.0.1-SNAPSHOT.jar
   ```
   로그 파일은 실행한 폴더의 `logs/sumoarchive.log`에 쌓인다 (`LOG_FILE` 환경변수로 경로 변경 가능).

3. **systemd 서비스 등록 예시 (`/etc/systemd/system/sumoarchive.service`)**:
   ```ini
   [Unit]
   Description=Sumoarchive Spring Boot Application
   After=network.target

   [Service]
   User=ubuntu
   WorkingDirectory=/home/ubuntu/sumoarchive
   EnvironmentFile=/home/ubuntu/sumoarchive/.env
   ExecStart=/usr/bin/java -jar build/libs/sumoarchive-0.0.1-SNAPSHOT.jar
   Restart=always
   RestartSec=10

   [Install]
   WantedBy=multi-user.target
   ```

---

## 4. 데이터베이스 스키마(Flyway)와 초기 데이터

### 1) 스키마는 Flyway가 만든다
- 마이그레이션 파일: `src/main/resources/db/migration/V{번호}__{설명}.sql`
- 앱이 뜰 때 Flyway가 아직 적용 안 된 버전을 순서대로 실행하고, 이어서 Hibernate가 엔티티와 테이블이 맞는지 검사(`ddl-auto=validate`)한다.
- **빈 DB**: `V1__init_schema.sql`부터 실행돼 테이블이 생성된다. 운영 DB는 별도로 CREATE TABLE 할 필요 없음.
- **Flyway 도입 전부터 테이블이 있던 DB**(로컬 등): 첫 기동 시 "V1까지 적용됨"으로 표시(baseline)만 하고 데이터는 건드리지 않는다.
- 엔티티에 컬럼을 추가/변경하면 반드시 `V2__add_xxx.sql` 같은 새 파일을 만든다. 이미 적용된 V 파일은 수정하지 않는다(체크섬이 달라져 기동 실패).

### 2) 로컬 데이터를 운영으로 옮기기 (최초 1회)
한국어 표기 검수, 헤야·이치몬, 과거 바쇼 임포트 결과는 로컬 DB에만 있으므로 처음 배포할 때 데이터를 덤프해서 넣는다.

```bash
# (로컬) 데이터만 덤프 - 스키마·flyway 이력·댓글 제외
MYSQL_PWD=<로컬 비밀번호> ./scripts/export-data.sh sumo-data.sql

# (서버) 앱을 한 번 띄워 Flyway가 테이블을 만들게 한 뒤, 데이터를 넣는다
docker compose up -d
docker exec -i sumoarchive-db mysql -u root -p<ROOT_PASS> --default-character-set=utf8mb4 sumo < sumo-data.sql

# 반즈케·바쇼 목록은 캐시되므로 데이터를 넣은 뒤 앱을 재시작해야 화면에 반영된다
docker compose restart app
```

---

## 5. Nginx 리버스 프록시 및 SSL 설정

### 1) Nginx 설정 파일 (`/etc/nginx/sites-available/sumoarchive`)

```nginx
server {
    listen 80;
    server_name your-domain.com www.your-domain.com;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name your-domain.com www.your-domain.com;

    # SSL 인증서 (Certbot / Let's Encrypt)
    ssl_certificate /etc/letsencrypt/live/your-domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;

    # 보안 헤더
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header Referrer-Policy "no-referrer" always;

    # Gzip 압축
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml;
    gzip_min_length 1000;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;

        # 원본 IP 전달 (Rate Limiter 연동에 필수).
        # 앱은 server.forward-headers-strategy=native로 X-Forwarded-For를 "오른쪽부터" 읽고 신뢰 프록시(사설망·localhost)만 건너뛴다.
        # $proxy_add_x_forwarded_for는 Nginx가 본 실제 IP를 맨 뒤에 붙이므로, 클라이언트가 헤더를 위조해 앞에 넣어도 무시된다.
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # 타임아웃 설정
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # 정적 리소스 브라우저 캐싱 (CSS, JS, 폰트)
    location ~* \.(css|js|woff2|woff|ttf|png|jpg|ico|svg)$ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        expires 7d;
        add_header Cache-Control "public, max-age=604800, immutable";
    }
}
```

### 2) 무료 SSL 인증서 발급 (Certbot)
```bash
sudo apt update && sudo apt install certbot python3-certbot-nginx -y
sudo certbot --nginx -d your-domain.com -d www.your-domain.com
```

### 3) 접속 로그 보관 기간 (14일)
개인정보 처리 안내(`/privacy`)에 "서버 접속 기록은 14일 후 자동 삭제"라고 적었으므로 Nginx 로그 보관 기간을 맞춘다.
Ubuntu 기본 `/etc/nginx/logrotate.d/nginx`는 보통 `daily` + `rotate 14`지만 배포판마다 다르니 확인한다.

```bash
grep -E "daily|weekly|rotate" /etc/nginx/logrotate.d/nginx   # daily / rotate 14 인지 확인
sudo nano /etc/nginx/logrotate.d/nginx                         # 다르면 daily, rotate 14로 고친다
sudo logrotate -d /etc/nginx/logrotate.d/nginx                 # 설정 점검 (실제로 돌리지 않음)
```

> 앱 로그(14일, `application-prod.properties`)와 DB 백업(14일, `scripts/backup-db.sh`)도 안내문과 같은 기간이다. 기간을 바꾸면 `templates/privacy.html`도 같이 고친다.

---

## 6. 데이터베이스 백업 및 복구 팁

관리자가 손으로 채운 데이터(최고위·한국어 표기 검수 등)와 댓글은 sumo-api에서 다시 받아올 수 없으므로 매일 자동으로 백업한다.

### 1) 자동 백업 (`scripts/backup-db.sh`)
- DB 전체(스키마·flyway 이력·댓글 포함)를 `/var/backups/sumoarchive/sumo_YYYYMMDD_HHMMSS.sql.gz`로 저장하고, 14일이 지난 파일은 지운다.
- 비밀번호는 컨테이너 안의 `MYSQL_ROOT_PASSWORD`를 환경변수로 넘긴다 (`-p<비밀번호>`는 `ps`로 누구나 볼 수 있어 쓰지 않는다).
- 덤프가 중간에 끊기면 파일을 남기지 않고 exit 1로 끝난다.
- 경로·보관 기간은 `BACKUP_DIR`, `KEEP_DAYS` 환경변수로 바꿀 수 있다.

```bash
# 한 번 수동 실행해서 확인
sudo ./scripts/backup-db.sh

# cron 등록 (sudo crontab -e) - 매일 새벽 4시
0 4 * * * /home/ubuntu/sumoarchive/scripts/backup-db.sh >> /var/log/sumoarchive-backup.log 2>&1
```

> 서버 디스크가 통째로 날아가면 백업도 같이 사라진다. 백업 폴더를 주기적으로 서버 밖(오브젝트 스토리지, 다른 PC 등)으로도 복사해 두자.
> 예: `rclone copy /var/backups/sumoarchive remote:sumoarchive-backup`

### 2) 복원
```bash
# 복원 중 앱이 쓰기를 하지 않도록 앱을 먼저 멈춘다
docker compose stop app

# 백업 파일에 CREATE DATABASE/USE가 들어 있으므로 DB 이름은 따로 지정하지 않는다
gunzip -c /var/backups/sumoarchive/sumo_20260930_040000.sql.gz \
  | docker exec -i sumoarchive-db sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -u root --default-character-set=utf8mb4'

# 캐시(반즈케·바쇼 목록)를 비우기 위해 앱을 다시 띄운다
docker compose start app
```

---

## 7. 운영 체크리스트 (배포 전 확인)

- [x] **운영 프로필 분리**: `application-prod.properties`의 `ddl-auto=validate`, `show-sql=false`, `thymeleaf.cache=true` 적용 완료
- [x] **스키마 버전 관리**: Flyway 도입, 빈 DB는 `V1__init_schema.sql`로 생성 / 기존 DB는 baseline (4절)
- [x] **클라이언트 IP 위조 방지**: X-Forwarded-For는 신뢰 프록시를 거친 경우만 반영 (`server.forward-headers-strategy=native`)
- [x] **관리자 로그인 보강**: 로그인 시 세션 ID 교체(세션 고정 방지), 일정 시간 비교, 관리자 POST에 CSRF 토큰 필수
- [x] **Docker 노출 최소화**: 앱 포트는 `127.0.0.1:8080`에만 바인딩, `ADMIN_PASSWORD`는 기본값 없이 `.env` 필수
- [x] **검색 노출**: 페이지 설명·OG·파비콘(`fragments/common :: seo`), 검색·즐겨찾기·에러 페이지 noindex, `/robots.txt`·`/sitemap.xml`(요청 주소 기준으로 생성 - Nginx가 `Host`·`X-Forwarded-Proto`를 넘겨야 https 도메인으로 나옴)
- [x] **출처·면책**: 공개 페이지 푸터에 sumo-api.com 출처와 "日本相撲協会와 무관한 개인 팬 사이트" 문구 (`fragments/common :: footerNote`)
- [ ] **배포 후 검색 등록**: Google Search Console·네이버 서치어드바이저에 사이트 등록 후 `https://도메인/sitemap.xml` 제출
- [x] **DB 비밀번호**: compose에 `SPRING_DATASOURCE_PASSWORD`·`MYSQL_ROOT_PASSWORD` 기본값 없음 - `.env`에 없으면 시작 거부 (DB 포트는 외부에 열지 않음)
- [x] **시크릿 환경변수화**: DB 및 어드민 비밀번호를 `.env` 또는 서버 환경변수로 관리
- [x] **데이터 안전성 확보**: 로스터 임포트 시 `wipeExisting()` 제거 및 `Upsert` 전환 완료
- [x] **보안 가드 탑재**: 익명 댓글 3초 쿨다운 & 1분 5회 제한, 관리자 로그인 5회 실패 차단, 세션 쿠키 SameSite=Lax 및 HttpOnly 적용
- [x] **댓글 PIN 댓글별 잠금**: IP와 상관없이 댓글 하나당 1시간 내 비밀번호 10회 오류 시 그 댓글 삭제 잠금 (IP 여러 개 사용·자기 댓글 삭제로 IP 기록 초기화하는 우회 방어). 인메모리라 앱 재시작 시 초기화
- [x] **댓글 신고**: 방문자 신고 버튼(같은 IP 같은 댓글 24시간 1회, IP당 10분 10건), 자동 숨김 없이 관리자 댓글 관리의 "신고된 댓글" 탭에서 신고 많은 순으로 확인 후 블라인드. 신고자 정보는 저장하지 않음 (Flyway `V2__add_comment_report.sql`, 배포 시 자동 적용)
- [x] **개인정보 처리 안내**: `/privacy`(한·일), 모든 페이지 푸터에서 연결. 문의 이메일은 `CONTACT_EMAIL`(필수). 본인 삭제 댓글은 원문 즉시 삭제(Flyway V3로 기존 것도 정리), YouTube는 youtube-nocookie 임베드
- [ ] **Nginx 접속 로그 14일 보관 확인**: 안내문과 맞추기 (5절 3)
- [x] **캐싱 최적화**: 바쇼 목록, 반즈케 데이터, 키마리테 백과사전에 Spring Cache 적용 완료
- [x] **관리자 수정 즉시 반영**: 바쇼·반즈케 행·리키시 프로필·헤야 이름 수정 시 관련 캐시를 커밋 직후 비움 (`config/CacheConfig`). DB에 SQL로 직접 넣은 데이터는 여전히 앱 재시작 필요
- [x] **이미지 핫링크 방어**: 템플릿 메타 태그 `<meta name="referrer" content="no-referrer">` 적용 완료
- [x] **에러 페이지 완성**: 404(`不見当`) 및 500(`物言い`) 맞춤형 에러 페이지 탑재
- [x] **헬스체크**: Actuator `/actuator/health`만 노출(세부 정보 숨김), compose app healthcheck 적용
- [x] **DB 자동 백업**: `scripts/backup-db.sh` + cron, 14일 보관 (6절)
- [x] **로그 보존**: docker logs 컨테이너당 10MB x 5개 제한, 앱 로그는 `sumo_app_logs` 볼륨에 파일로 저장(14일·500MB, 날짜·10MB 단위 gzip)
- [ ] **백업 외부 보관 / 업타임 모니터 등록**: 백업 폴더를 서버 밖으로 복사, `/actuator/health`를 외부 모니터에 등록
