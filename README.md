# 서비 (Subby)

구독을 챙겨주는 작은 비서. OTT·AI·앱 정기 결제를 한곳에 등록하고, 이번 달 얼마가 언제 나가는지 보여주는 웹 서비스입니다.

- 배포 주소: `https://subby-xxxx.onrender.com` (배포 후 실제 주소로 바꾸세요)
- 무료 서버라 15분 동안 접속이 없으면 잠듭니다. **첫 접속은 최대 1분 정도 걸릴 수 있습니다.**

## 주요 기능

| 영역 | 기능 |
| --- | --- |
| 회원 | 이메일 가입·로그인, Google·카카오 로그인, 로그인 수단 연결·해제, 비밀번호 재설정(메일), 5회 실패 시 15분 잠금 |
| 구독 | 등록·수정·삭제·해지, 매주·매월·매년 결제, 원화·달러 결제 |
| 지출 | 다음 결제일, 이번 달 결제 예정액, 월 평균 환산액, 최근 6·12개월 월별 지출, 카테고리별 지출 |
| 알림 | 결제 1·3·7일 전 메일 알림, 메일에서 바로 알림 끄기 |

## 기술 스택

- **Backend**: Java 21, Spring Boot 3.5, Spring Security, Spring Session JDBC, Spring Data JPA, Flyway, PostgreSQL 17
- **Frontend**: React 19, React Router, Vite, Recharts
- **Test**: JUnit 5, Mockito, Testcontainers(PostgreSQL)
- **Infra**: Docker, Render(서버), Neon(DB), GitHub Actions(CI/CD, 예약 실행), Brevo(메일), Frankfurter(환율)

## 구조

```
사용자 브라우저 ──HTTPS──> Render (Spring Boot + React 빌드 파일) ──SSL──> Neon PostgreSQL
                                   ▲
GitHub ── PR·병합 ──> GitHub Actions (빌드·테스트) ── 통과 시 배포 훅 호출
                      GitHub Actions (매일 09:17) ── POST /internal/jobs/daily (환율 수집 + 결제 알림)
```

## 설계 결정

| 결정 | 이유 |
| --- | --- |
| 프론트와 백엔드를 **한 서버로 배포** | 주소가 하나라 CORS, 사이트 간 쿠키 문제, 배포 두 군데 관리가 사라진다 |
| **세션 로그인 + DB 저장** (JWT 아님) | HttpOnly 쿠키라 스크립트가 탈취할 수 없고, 로그아웃·비밀번호 변경 시 서버에서 즉시 끊을 수 있다. 무료 서버는 자주 재시작되므로 세션을 메모리가 아닌 DB에 둔다 |
| **CSRF 쿠키 방식** | 세션 쿠키를 쓰면 다른 사이트가 사용자 몰래 요청을 보낼 수 있다. XSRF-TOKEN 쿠키 값을 헤더로 보내게 해서 막는다 |
| 같은 이메일 소셜 계정 **자동 합치기 안 함** | 가입 시 이메일 인증을 하지 않으므로, 남의 이메일로 먼저 가입해둔 공격자에게 계정이 넘어갈 수 있다. 연결은 로그인 상태에서만 |
| 다른 사람 구독 요청은 **404** | 403을 주면 그 ID가 존재한다는 사실이 드러난다. 모든 조회에 `user_id` 조건을 건다 |
| 로그인 실패 메시지 하나로 통일, 없는 이메일도 실패 횟수 기록 | 가입된 이메일을 알아낼 수 없게 한다 |
| 금액은 **BigDecimal + numeric**, 통화를 함께 저장 | double은 0.1을 정확히 못 담아 합계가 틀어진다. 달러는 센트 단위가 있다 |
| 다음 결제일은 **저장하지 않고 계산** | 매일 갱신하는 작업이 필요 없다. 첫 결제일 기준으로 계산해서 31일 구독이 28일로 굳지 않는다 |
| 지난 결제는 결제일 환율, 앞으로의 결제는 최신 환율 | 과거 통계가 오늘 환율에 따라 바뀌지 않게 한다 |
| 예약 작업을 **서버 스케줄러가 아니라 GitHub Actions**가 호출 | 무료 서버는 잠들면 스케줄러도 멈춘다. 외부 호출이 서버를 깨우면서 작업을 시킨다 |
| 알림은 "N일 이내 + 미발송" 조건 + DB 유니크 제약 | 예약 실행이 하루 빠져도 다음 날 알림이 가고, 두 번 실행돼도 중복 메일이 없다 |
| 메일은 SMTP가 아닌 **Brevo HTTP API** | Render 무료 서버는 SMTP 포트가 막혀 있다 |
| 비밀번호 재설정 토큰은 **해시만 저장**, 30분, 1회용, 재설정 시 모든 세션 종료 | DB가 유출돼도 링크를 만들 수 없고, 탈취된 로그인도 끊긴다 |
| Spring Boot 3.5 | 4.x는 설정 방식이 많이 바뀌었다. 3.5는 2026년 6월 오픈소스 지원이 끝났으므로 다음 단계로 4.x 전환이 필요하다 |

### 결제일 계산 규칙

- n번째 결제일 = 첫 결제일 + n주기. 그 달에 같은 날짜가 없으면 말일 (1/31 → 2/28 → 3/31)
- 2월 29일 연간 구독은 평년에 2월 28일
- 해지일 **당일과 그 이후** 결제는 계산하지 않는다
- 날짜는 한국 시간 기준 (서버는 UTC)

## 로컬 실행

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
- 예약 작업 수동 실행: `curl -X POST -H "X-Job-Token: local-job-token" http://localhost:8080/internal/jobs/daily`
- API 문서(로컬만): http://localhost:8080/swagger-ui.html

## 테스트

```bash
./gradlew test   # Docker Desktop이 켜져 있어야 함 (Testcontainers)
```

## 배포

[docs/SETUP.md](docs/SETUP.md)에 Render, Neon, Google, 카카오, Brevo, GitHub 설정 순서를 정리했습니다.

## API

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| POST | `/api/auth/signup` | 회원가입 (가입 후 바로 로그인) |
| POST | `/api/auth/login` | 로그인 |
| POST | `/api/auth/logout` | 로그아웃 |
| GET | `/oauth2/authorization/{google\|kakao}` | 소셜 로그인 시작 (로그인 상태면 연결) |
| POST | `/api/auth/password-reset/request` | 재설정 메일 요청 |
| POST | `/api/auth/password-reset/confirm` | 새 비밀번호 설정 |
| GET / PATCH | `/api/me` | 내 정보 / 닉네임 수정 |
| GET | `/api/me/identities` | 연결된 소셜 계정 |
| DELETE | `/api/me/identities/{provider}` | 소셜 연결 해제 |
| GET / PUT | `/api/me/notification-settings` | 알림 설정 |
| GET / POST | `/api/subscriptions` | 구독 목록(status, category 필터) / 등록 |
| GET / PUT / DELETE | `/api/subscriptions/{id}` | 구독 상세 / 수정 / 삭제 |
| POST | `/api/subscriptions/{id}/cancel` | 해지 |
| GET | `/api/dashboard` | 대시보드 |
| GET | `/api/stats/monthly?months=6` | 월별 결제액 |
| GET | `/api/stats/categories?yearMonth=2026-11` | 카테고리별 결제액 |
| POST | `/api/notifications/unsubscribe` | 메일 링크로 알림 끄기 |
| POST | `/internal/jobs/daily` | 환율 수집 + 결제 알림 (`X-Job-Token` 헤더) |
| GET | `/actuator/health` | 상태 확인 |

에러 응답은 RFC 9457 ProblemDetail 형식이며 `code`(분기용), `detail`(사용자 메시지), `errors`(입력 항목별 메시지)를 담습니다.
