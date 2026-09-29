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
   docker compose logs -f app
   ```

3. **컨테이너 중지 / 재시작**:
   ```bash
   docker compose down
   docker compose restart app
   ```

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

---

## 6. 데이터베이스 백업 및 복구 팁

운영 중 주기적인 데이터 백업은 필수입니다.

```bash
# 1. 백업 (덤프)
docker exec -i sumoarchive-db mysqldump -u root -p<ROOT_PASS> sumo > backup_$(date +%Y%m%d).sql

# 2. 복원
docker exec -i sumoarchive-db mysql -u root -p<ROOT_PASS> sumo < backup_20260924.sql
```

---

## 7. 운영 체크리스트 (배포 전 확인)

- [x] **운영 프로필 분리**: `application-prod.properties`의 `ddl-auto=validate`, `show-sql=false`, `thymeleaf.cache=true` 적용 완료
- [x] **스키마 버전 관리**: Flyway 도입, 빈 DB는 `V1__init_schema.sql`로 생성 / 기존 DB는 baseline (4절)
- [x] **클라이언트 IP 위조 방지**: X-Forwarded-For는 신뢰 프록시를 거친 경우만 반영 (`server.forward-headers-strategy=native`)
- [x] **관리자 로그인 보강**: 로그인 시 세션 ID 교체(세션 고정 방지), 일정 시간 비교, 관리자 POST에 CSRF 토큰 필수
- [x] **Docker 노출 최소화**: 앱 포트는 `127.0.0.1:8080`에만 바인딩, `ADMIN_PASSWORD`는 기본값 없이 `.env` 필수
- [ ] **DB 비밀번호**: compose의 `SPRING_DATASOURCE_PASSWORD`·`MYSQL_ROOT_PASSWORD` 기본값 대신 `.env`에 강한 값 설정 (DB 포트는 외부에 열지 않음)
- [x] **시크릿 환경변수화**: DB 및 어드민 비밀번호를 `.env` 또는 서버 환경변수로 관리
- [x] **데이터 안전성 확보**: 로스터 임포트 시 `wipeExisting()` 제거 및 `Upsert` 전환 완료
- [x] **보안 가드 탑재**: 익명 댓글 3초 쿨다운 & 1분 5회 제한, 관리자 로그인 5회 실패 차단, 세션 쿠키 SameSite=Lax 및 HttpOnly 적용
- [x] **캐싱 최적화**: 바쇼 목록, 반즈케 데이터, 키마리테 백과사전에 Spring Cache 적용 완료
- [x] **이미지 핫링크 방어**: 템플릿 메타 태그 `<meta name="referrer" content="no-referrer">` 적용 완료
- [x] **에러 페이지 완성**: 404(`不見当`) 및 500(`物言い`) 맞춤형 에러 페이지 탑재
