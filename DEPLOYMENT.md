# SUMOARCHIVE 운영 서버 배포 가이드 (Production Deployment Guide)

이 문서는 **SUMOARCHIVE**를 실제 운영 서버(AWS, GCP, Oracle Cloud, VPS 등)에 배포하고 운영하기 위한 실전 가이드입니다.
처음 배포할 때는 0절을 위에서부터 순서대로 따라 하고, 자세한 내용은 각 단계에 적힌 절을 본다.

---

## 0. 처음 배포하기 (순서대로)

### 준비할 것
- **서버**: Ubuntu 24.04, **메모리 2GB 이상** (서버에서 Docker 이미지를 빌드할 때 Gradle 컴파일 + JVM + MySQL이 함께 돈다. 1GB면 아래 스왑 필수)
- **도메인**과 DNS 관리 화면 접근
- **문의 이메일** (개인정보 처리 안내 `/privacy`에 공개된다)
- 로컬 PC: 최신 `main`, 로컬 DB(데이터 이전용), `mysqldump`

### 1단계: 서버 기본 설정
```bash
# (서버, root 또는 sudo 사용자) 작업용 사용자 - SSH 키로 로그인되는 것을 확인한 뒤 비밀번호 로그인을 끈다
sudo adduser deploy && sudo usermod -aG sudo deploy

# 시간대: 백업 파일 이름·cron 시각이 한국 시간이 되도록 (앱 JVM은 Dockerfile에서 따로 Asia/Seoul)
sudo timedatectl set-timezone Asia/Seoul

# 방화벽: SSH·HTTP·HTTPS만 (앱 8080·DB 3306은 밖에 열지 않는다)
sudo apt update && sudo apt install -y ufw nginx certbot python3-certbot-nginx git
sudo ufw allow OpenSSH && sudo ufw allow 'Nginx Full' && sudo ufw enable

# 메모리 1~2GB 서버면 스왑 2GB (이미지 빌드 중 메모리 부족 방지)
sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# Docker + compose 플러그인, deploy 사용자가 sudo 없이 docker를 쓰도록 (다시 로그인해야 적용)
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker deploy
```

### 2단계: 코드와 `.env` (→ 2절)
```bash
git clone https://github.com/genuinekumquat/sumoarchive.git ~/sumoarchive && cd ~/sumoarchive
cp .env.example .env && chmod 600 .env
openssl rand -base64 24     # 비밀번호가 필요할 때마다 실행해 붙여 넣는다
nano .env                   # SPRING_DATASOURCE_PASSWORD, MYSQL_ROOT_PASSWORD, ADMIN_USERNAME, ADMIN_PASSWORD, CONTACT_EMAIL
```
- 비밀번호 3개(`SPRING_DATASOURCE_PASSWORD`·`MYSQL_ROOT_PASSWORD`·`ADMIN_PASSWORD`)와 `CONTACT_EMAIL` 중 하나라도 비어 있으면 compose가 시작을 거부한다 (의도된 동작).
- `.env`의 `SPRING_DATASOURCE_URL`은 compose가 쓰지 않는다 (compose 안에서 `db:3306`으로 고정).

### 3단계: 앱 기동과 데이터 이전 (→ 4절 2)
1. (로컬) `MYSQL_PWD=<로컬 비밀번호> ./scripts/export-data.sh sumo-data.sql` → `scp`로 서버 `~/sumoarchive/`에
2. (서버) `docker compose up -d --build` — 처음 빌드는 몇 분 걸린다. `docker compose ps`에서 app이 `(healthy)`가 될 때까지 기다린다 (Flyway가 빈 DB에 V1~ 적용)
3. (서버) 4절 2)의 `docker exec … mysql … < sumo-data.sql`로 데이터를 넣고 `docker compose restart app`
4. 확인: `curl -s 127.0.0.1:8080/actuator/health` → `{"status":"UP"}`, `curl -s "127.0.0.1:8080/api/banzuke?division=Makuuchi" | head -c 200`
- 이 단계까지는 Nginx가 없어서 밖에서 사이트가 안 보인다 → 빈 화면이 공개되거나 캐시되는 일이 없다.

### 4단계: 도메인과 HTTPS (→ 5절)
1. DNS에 A 레코드(`@`, `www` → 서버 IP) 추가, `ping your-domain.com`으로 반영 확인
2. 5절 0)의 순서: 80 포트 임시 설정 → `certbot --nginx` → 5절 1)의 최종 설정(보안 헤더, `/admin/` 300초)으로 교체
3. 5절 3)으로 Nginx 접속 로그 14일 보관 확인

### 5단계: 운영 설정 (→ 3절, 6절)
- 백업: `sudo ./scripts/backup-db.sh` 한 번 실행해 파일이 생기는지 보고, `sudo crontab -e`로 매일 새벽 4시 등록 (6절 1)
- 백업을 서버 밖으로도 복사 (rclone 등)
- 업타임 모니터(UptimeRobot 등)에 `https://your-domain.com/actuator/health` 등록

### 6단계: 공개 전 확인
- [ ] `https://` 접속, `http://`는 https로 넘어감
- [ ] 메인(반즈케·일문·키마리테 탭), 리키시 프로필, 경기 상세, 검색, 즐겨찾기 — 한국어·일본어(`?lang=ja`) 모두
- [ ] 관리자 로그인(`/admin/login`), 댓글 작성·신고·본인 삭제, 관리자 블라인드
- [ ] 댓글 작성 시각이 한국 시간으로 나오는지
- [ ] `/privacy`에 실제 문의 이메일, 시행일
- [ ] `/robots.txt`·`/sitemap.xml`의 주소가 `https://your-domain.com`으로 나오는지 (Nginx가 Host·X-Forwarded-Proto를 넘겨야 함)
- [ ] `docker compose exec app ls -l /app/logs`에 로그 파일

### 7단계: 공개 후
- Google Search Console·네이버 서치어드바이저에 사이트 등록, `https://your-domain.com/sitemap.xml` 제출
- 다음 바쇼 반즈케 발표 때 7절 절차대로 첫 데이터 갱신

### 이후 업데이트 배포
```bash
cd ~/sumoarchive
sudo ./scripts/backup-db.sh             # 먼저 백업
git pull
docker compose up -d --build            # 새 이미지로 교체, Flyway 새 마이그레이션은 자동 적용 (1분 안팎 중단)
docker compose ps && curl -s 127.0.0.1:8080/actuator/health
```
- main에 푸시된 커밋은 GitHub Actions(CI)가 테스트·이미지 빌드를 먼저 확인한다. CI가 실패한 커밋은 배포하지 않는다.

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
# (로컬) 데이터만 덤프 - 스키마·flyway 이력·댓글 제외 (mysqldump가 PATH에 있어야 한다)
MYSQL_PWD=<로컬 비밀번호> ./scripts/export-data.sh sumo-data.sql
scp sumo-data.sql <사용자>@<서버>:~/sumoarchive/

# (서버) 앱을 한 번 띄워 Flyway가 테이블을 만들게 한 뒤(healthy가 될 때까지 기다림), 데이터를 넣는다
docker compose up -d --build
docker compose ps                       # app이 (healthy)가 되면 다음 줄
docker exec -i sumoarchive-db sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -u root --default-character-set=utf8mb4 sumo' < sumo-data.sql

# 반즈케·바쇼 목록은 캐시되므로 데이터를 넣은 뒤 앱을 재시작해야 화면에 반영된다
docker compose restart app
```

- 비밀번호는 컨테이너 안의 환경변수로 넘긴다 (`-p<비밀번호>`는 `ps`로 보이므로 쓰지 않는다).
- 2026-09-30 로컬 리허설: 빈 DB에 V1~V3 적용 → 덤프(3.6MB) 넣기 → 바쇼 46·반즈케 3,685·대전 22,530·리키시 682·헤야 52·수상 428·킨보시 89 원본과 일치.

---

## 5. Nginx 리버스 프록시 및 SSL 설정

### 0) 처음 설정 순서 — 인증서부터
아래 1)의 최종 설정은 443 블록이 인증서 파일(`/etc/letsencrypt/live/…`)을 가리키므로, **인증서가 없을 때 넣으면 `nginx -t`가 실패해 Nginx가 뜨지 않는다.**
처음에는 80 포트만 열고 인증서를 받은 뒤 최종 설정으로 바꾼다. (DNS A 레코드가 서버 IP를 가리키고 있어야 한다)

```bash
# 1. 80 포트만 있는 임시 설정
sudo tee /etc/nginx/sites-available/sumoarchive > /dev/null <<'NGINX'
server {
    listen 80;
    server_name your-domain.com www.your-domain.com;
    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
    }
}
NGINX
sudo ln -sf /etc/nginx/sites-available/sumoarchive /etc/nginx/sites-enabled/sumoarchive
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t && sudo systemctl reload nginx

# 2. 인증서 발급 (자동 갱신 타이머도 같이 설치된다)
sudo certbot --nginx -d your-domain.com -d www.your-domain.com

# 3. 아래 1)의 최종 설정으로 파일을 통째로 바꾼 뒤
sudo nginx -t && sudo systemctl reload nginx
```

### 1) Nginx 설정 파일 (`/etc/nginx/sites-available/sumoarchive`) — 최종

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

    # 관리자 화면: sumo-api 임포트가 한 요청 안에서 끝나므로 제한 시간을 늘린다.
    # (측정: 바쇼 1개 일괄 임포트 약 17초, 로스터 약 1.4초 / 연 단위 일괄 임포트는 바쇼 6개라 100초 안팎)
    # 60초를 넘기면 브라우저엔 504가 뜨지만 서버에서는 임포트가 계속 돌아 결과를 알 수 없게 된다.
    location /admin/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 300s;
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

### 2) 무료 SSL 인증서 (Certbot)
발급은 0)의 순서대로 한다. 설치와 자동 갱신 확인:
```bash
sudo apt update && sudo apt install certbot python3-certbot-nginx -y
sudo certbot renew --dry-run          # 90일마다 자동 갱신되는지 미리 확인
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
0 4 * * * /home/deploy/sumoarchive/scripts/backup-db.sh >> /var/log/sumoarchive-backup.log 2>&1
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

## 7. 바쇼 운영 절차 (새 바쇼 데이터 넣기)

바쇼는 1·3·5·7·9·11월, 둘째 일요일부터 15일간 열린다. 반즈케는 보통 개막 약 2주 전 월요일에 발표된다.
(예: 2026년 11월 큐슈바쇼 = 11/8~11/22, 반즈케 발표 10/26 전후)

모든 임포트는 여러 번 눌러도 안전하다(있는 건 갱신, 없는 것만 추가). 임포트가 끝나면 메인 캐시는 자동으로 비워진다.

### 1) 반즈케 발표 후 (개막 전)
1. **DB 백업**: `sudo ./scripts/backup-db.sh` — 문제가 생기면 6절 방법으로 되돌린다.
2. **관리자 → 리키시 관리 → "sumo-api에서 로스터 임포트"**: 새로 입문·승진한 선수와 헤야를 들여온다.
3. **관리자 → 반즈케 관리 → "바쇼 메타 임포트"** (해당 연도): 새 바쇼가 목록에 생긴다.
   - sumo-api에 아직 일정이 없으면 "데이터 없음"으로 건너뛴다 → 발표가 반영될 때까지 기다린다.
4. **새 바쇼 → "⚡ 전체 일괄 임포트 →"**: 마쿠우치·쥬료 반즈케 + (있으면) 토리쿠미·수상. 약 20초.
   - 결과의 경고 목록에 "(api id=…)"로 매칭 안 된 선수가 있으면 2번 로스터 임포트를 다시 한 뒤 재실행.
5. **한국어 이름 검수**: 리키시 관리에서 "(자동)" 표시가 붙은 선수(로마자 자동 음차)를 확인해 고친다. 헤야도 새로 생겼으면 헤야 관리에서 같은 방식.
6. **확인**: 메인이 새 바쇼 반즈케로 바뀌었는지, 일문 탭, 새로 올라온 선수 프로필.
   - 반즈케가 들어가기 전에는 메인이 이전 바쇼를 계속 보여준다(반즈케 없는 바쇼는 드롭다운·기본 표시에서 빠짐). 바쇼만 먼저 생겨도 메인이 비지 않는다.

### 2) 대회 중 (15일간, 매일)
- 마쿠우치 경기는 18시 전후에 끝난다. 그 뒤 **해당 바쇼 → 토리쿠미 관리 → 디비전별 "이 디비전 임포트 (15일)"** (또는 "⚡ 전체 일괄 임포트 →").
  - 결과가 확정된 경기만 들어가고, 아직 안 치른 날은 건너뛴다(27편). 매일 다시 눌러도 된다.
- 확인: 호시토리표(진행 중 바쇼는 미개최일이 휴장으로 잡히지 않음), 경기 상세.

### 3) 폐막 후
1. **"⚡ 전체 일괄 임포트 →"** 한 번 더 (마지막 날·결정전 반영).
2. **"우승·삼상·킨보시 갱신 →"**: 수상은 폐막 후에 sumo-api에 반영된다.
3. 확인: 우승자 프로필의 수상 표시, 바쇼별 성적 요약, 메인 드롭다운.
4. DB 백업 (`backup-db.sh`, cron이 있어도 한 번).

### 문제가 생기면
- 504 Gateway Timeout: Nginx `/admin/` 제한 시간(5절, 300초) 확인. 서버에선 임포트가 계속 돌았을 수 있으니 앱 로그(`/app/logs`)에서 `[BashoBatchImport] … 완료`를 확인한 뒤 필요하면 다시 누른다.
- 데이터가 이상하면: 1)-1의 백업으로 복원(6절) 후 앱 재시작.

---

## 8. 운영 체크리스트 (배포 전 확인)

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
- [x] **빈 바쇼 방지**: 반즈케 없는 바쇼는 메인 드롭다운·기본 표시·일문 탭에서 제외 (바쇼만 먼저 등록돼도 메인이 비지 않음)
- [x] **시간대**: 컨테이너 JVM `-Duser.timezone=Asia/Seoul` (댓글 작성·신고 시각, 로그), 서버는 `timedatectl set-timezone Asia/Seoul` (백업 파일명·cron) — CI가 이미지의 JVM 시간대 확인
- [ ] **Nginx `/admin/` 제한 시간 300초**: 5절 설정 (임포트가 60초를 넘길 수 있음)
- [ ] **첫 바쇼 리허설**: 7절 절차대로 (2026년 11월 큐슈바쇼 반즈케 발표 10/26 전후)
- [x] **캐싱 최적화**: 바쇼 목록, 반즈케 데이터, 키마리테 백과사전에 Spring Cache 적용 완료
- [x] **관리자 수정 즉시 반영**: 바쇼·반즈케 행·리키시 프로필·헤야 이름 수정 시 관련 캐시를 커밋 직후 비움 (`config/CacheConfig`). DB에 SQL로 직접 넣은 데이터는 여전히 앱 재시작 필요
- [x] **이미지 핫링크 방어**: 템플릿 메타 태그 `<meta name="referrer" content="no-referrer">` 적용 완료
- [x] **에러 페이지 완성**: 404(`不見当`) 및 500(`物言い`) 맞춤형 에러 페이지 탑재
- [x] **헬스체크**: Actuator `/actuator/health`만 노출(세부 정보 숨김), compose app healthcheck 적용
- [x] **DB 자동 백업**: `scripts/backup-db.sh` + cron, 14일 보관 (6절)
- [x] **로그 보존**: docker logs 컨테이너당 10MB x 5개 제한, 앱 로그는 `sumo_app_logs` 볼륨에 파일로 저장(14일·500MB, 날짜·10MB 단위 gzip)
- [ ] **백업 외부 보관 / 업타임 모니터 등록**: 백업 폴더를 서버 밖으로 복사, `/actuator/health`를 외부 모니터에 등록
