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

# 파일 소유권 변경
RUN chown appuser:appgroup app.jar

USER appuser

# 기본 운영 포트 (8080)
EXPOSE 8080

# JVM 메모리 및 인코딩 최적화 옵션
ENV JAVA_OPTS="-Dfile.encoding=UTF-8 -XX:+UseG1GC -XX:MaxRAMPercentage=75.0"
ENV SPRING_PROFILES_ACTIVE="prod"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
