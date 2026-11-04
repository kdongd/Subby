# 서비 로컬 개발

## 실행

필요: JDK 21, Node.js 22, Docker Desktop

```bash
# 1. DB 실행
docker compose up -d

# 2. 백엔드 실행 (http://localhost:8080)
./gradlew bootRun

# 3. 프론트 개발 서버 (http://localhost:5173, API는 8080으로 전달)
cd frontend
npm install
npm run dev
```

- 메일 키가 없으면 메일 내용이 서버 로그에 출력됩니다. 비밀번호 재설정 링크도 로그에서 확인할 수 있습니다.
- 소셜 로그인을 로컬에서 쓰려면 환경변수 `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET`을 설정하고, 콘솔에 `http://localhost:8080/login/oauth2/code/{google|kakao}`를 등록한 뒤 `npm run build` 후 8080으로 접속하세요.
- 기기 푸시를 로컬에서 쓰려면 `npx web-push generate-vapid-keys`로 키를 만들어 환경변수 `VAPID_PUBLIC_KEY`, `VAPID_PRIVATE_KEY`를 설정하세요. 키가 없어도 사이트 안 알림(🔔)은 동작합니다. 서비스 워커는 `npm run build` 후 8080으로 접속해야 확인할 수 있습니다(localhost는 https가 아니어도 허용됨).
- 예약 작업 수동 실행: `curl -X POST -H "X-Job-Token: local-job-token" http://localhost:8080/internal/jobs/daily`
- API 문서(로컬만): http://localhost:8080/swagger-ui.html

## 테스트

```bash
./gradlew test   # Docker Desktop이 켜져 있어야 함 (Testcontainers)
```
