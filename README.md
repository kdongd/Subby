# 🐧 서비 (Subby)

> **나의 구독, 놓치지 않게 챙겨주는 펭귄 비서**
>
> 흩어진 구독을 한곳에 모아, 결제일 전에 알려주고 매달 얼마 쓰는지 보여주는 구독 관리 서비스

🔗 **https://subby-xrje.onrender.com**

> 무료 서버라 한동안 접속이 없으면 잠들어요. 첫 접속은 최대 1분 정도 걸릴 수 있어요.

---

## 📌 프로젝트 소개

**서비 (Subby)** 는 넷플릭스, 유튜브 프리미엄, ChatGPT 같은 정기결제를 한곳에서 관리하는 웹 서비스입니다.
휴대폰 홈 화면에 추가하면 앱처럼 쓸 수 있습니다 (PWA).

- 🗂 **구독 관리** — 매주·매월·매년 결제, 원화·달러 결제를 한곳에 등록
- 🔔 **결제 알림** — 결제 1·3·7일 전에 휴대폰 푸시와 사이트 알림으로 안내
- 📊 **지출 통계** — 이번 달 결제액, 월별 추이, 카테고리별 지출
- 🔐 **간편 로그인** — Google·카카오로 바로 시작

---

## 🛠 기술 스택

### Backend

![Java](https://img.shields.io/badge/Java%2021-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot%203.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring%20Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![JPA](https://img.shields.io/badge/Spring%20Data%20JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white)

### Frontend

![React](https://img.shields.io/badge/React%2019-61DAFB?style=for-the-badge&logo=react&logoColor=black)
![Vite](https://img.shields.io/badge/Vite-646CFF?style=for-the-badge&logo=vite&logoColor=white)
![PWA](https://img.shields.io/badge/PWA-5A0FC8?style=for-the-badge&logo=pwa&logoColor=white)

### Database

![PostgreSQL](https://img.shields.io/badge/PostgreSQL%2017-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Neon](https://img.shields.io/badge/Neon-00E599?style=for-the-badge&logo=neon&logoColor=black)
![Flyway](https://img.shields.io/badge/Flyway-CC0200?style=for-the-badge&logo=flyway&logoColor=white)

### Infra & CI/CD

![Render](https://img.shields.io/badge/Render-46E3B7?style=for-the-badge&logo=render&logoColor=black)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/GitHub%20Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white)
![JUnit5](https://img.shields.io/badge/JUnit5-25A162?style=for-the-badge&logo=junit5&logoColor=white)
![Testcontainers](https://img.shields.io/badge/Testcontainers-2496ED?style=for-the-badge&logo=docker&logoColor=white)

---

## ✅ 주요 구현 기능

| 구분 | 주요 기능 |
|------|-----------|
| 🔐 로그인 | Google·카카오 로그인, 처음 로그인 시 자동 가입, 로그인 수단 연결·해제 |
| 🗂 구독 | 구독 등록·수정·해지·삭제, 자주 쓰는 서비스 선택, 서비스 로고 표시 |
| 📅 결제일 | 첫 결제일 또는 다음 갱신일로 입력, 다음 결제일 자동 계산 |
| 💱 환율 | 달러 결제를 원화로 환산 (지난 결제는 결제일 환율, 앞으로의 결제는 최신 환율) |
| 🏠 대시보드 | 이번 달 구독료, 7일 안에 결제될 구독, 카테고리 비율 |
| 📊 통계 | 월별 지출 추이, 카테고리별 지출 |
| 🔔 알림 | 결제 1·3·7일 전 웹 푸시 + 사이트 안 알림, 기기별 푸시 켜기·끄기 |
| 📱 PWA | 홈 화면에 추가해 앱처럼 사용, 아이폰·안드로이드 푸시 지원 |

---

## 🚀 배포 구조

```text
사용자 ──HTTPS──▶ Render (Spring Boot + React) ──▶ Neon PostgreSQL

GitHub PR ──▶ GitHub Actions (빌드·테스트) ──통과 후 병합──▶ Render 자동 배포
GitHub Actions (매일 오전 9시) ──▶ 환율 수집 + 결제 알림 발송
```

- 프론트와 백엔드를 **한 서버로 배포**해서 주소 하나로 운영
- **테스트를 통과한 코드만** main에 병합되고 자동 배포
- 무료 서버가 잠들어도 매일 알림이 가도록 **예약 작업은 GitHub Actions가 호출**

---

## 💡 기술적으로 신경 쓴 부분

- **세션 로그인 + DB 저장** — HttpOnly 쿠키로 토큰 탈취를 막고, 서버가 자주 재시작돼도 로그인이 유지
- **금액은 BigDecimal** — 원화·달러 합계에서 소수점 오차가 생기지 않게
- **결제일은 저장하지 않고 계산** — 1월 31일 구독이 2월 28일 → 3월 31일로 정확히 돌아오게
- **같은 결제는 한 번만 알림** — DB 유니크 제약으로 예약 작업이 두 번 돌아도 중복 알림 없음
- **웹 푸시 암호화를 JDK만으로 구현** — 표준(RFC 8291·8292)과 같은 결과가 나오는지 테스트로 검증
- **다른 사람의 구독은 404** — 존재 여부조차 드러나지 않게 모든 조회에 사용자 조건

상세 설계 기록은 [docs/DESIGN.md](docs/DESIGN.md)에 있습니다.

---

## 🧪 테스트

- JUnit 5, Mockito 기반 단위 테스트
- MockMvc 기반 API·인증 테스트
- Testcontainers로 실제와 같은 PostgreSQL에서 통합 테스트
- PR마다 GitHub Actions에서 자동 실행, 실패하면 병합 불가

---

## 📄 문서

- [로컬 개발 방법](docs/DEVELOPMENT.md)
- [배포·외부 서비스 설정](docs/SETUP.md)
- [설계 결정](docs/DESIGN.md)
