# =====================================================
# Stage 1: Build JAR using Gradle
# =====================================================
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Gradle 래퍼 및 설정 복사
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./

# 실행 권한 부여 및 의존성 사전 캐싱
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

# 소스 코드 복사 및 애플리케이션 빌드 (테스트는 CI 또는 로컬에서 선검증 후 빌드)
COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test

# =====================================================
# Stage 2: Production JRE Runtime
# =====================================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 보안을 위해 비-루트(non-root) 시스템 유저 생성
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# 빌더 스테이지에서 생성된 JAR 파일 복사
COPY --from=builder /workspace/build/libs/*.jar app.jar

# 파일 소유권 변경 + 로그 폴더 생성 (appuser가 써야 하므로 소유자를 맞춘다.
# compose의 이름 있는 볼륨은 처음 붙을 때 이 폴더의 소유권을 그대로 물려받는다)
RUN chown appuser:appgroup app.jar \
 && mkdir -p /app/logs && chown appuser:appgroup /app/logs

USER appuser

# 기본 운영 포트 (8080)
EXPOSE 8080

# JVM 메모리 및 인코딩 최적화 옵션
# user.timezone: 컨테이너 기본은 UTC라 댓글 작성·신고 시각(LocalDateTime.now, @CreationTimestamp)과
# 로그 시각이 한국 시간보다 9시간 늦게 찍힌다. JVM 자체 시간대 DB를 쓰므로 alpine에 tzdata가 없어도 된다.
ENV JAVA_OPTS="-Dfile.encoding=UTF-8 -Duser.timezone=Asia/Seoul -XX:+UseG1GC -XX:MaxRAMPercentage=75.0"
ENV SPRING_PROFILES_ACTIVE="prod"
# 로그 파일 위치 (application-prod.properties의 logging.file.name)
ENV LOG_FILE="/app/logs/sumoarchive.log"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
