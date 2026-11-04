# 1단계: React 빌드 → src/main/resources/static
FROM node:22-alpine AS frontend
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# 2단계: React 빌드 결과를 포함해 Spring Boot jar 빌드
FROM eclipse-temurin:21-jdk AS backend
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null
COPY src src
COPY --from=frontend /app/src/main/resources/static src/main/resources/static
# 테스트는 GitHub Actions에서 이미 통과한 코드만 배포되므로 여기서는 건너뛴다.
RUN ./gradlew --no-daemon bootJar -x test

# 3단계: 실행에 필요한 JRE와 jar 하나만 담은 가벼운 이미지
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 subby
COPY --from=backend /app/build/libs/app.jar app.jar
USER subby
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-jar", "app.jar"]
