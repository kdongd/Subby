# 서비 배포·외부 서비스 설정 가이드

계정을 만들고 비밀값(비밀번호, 키)을 입력하는 작업은 직접 해야 합니다. 이 문서는 그 순서를 정리한 것입니다.
**비밀값은 코드나 GitHub 저장소에 절대 커밋하지 마세요.** Render 환경변수와 GitHub Secrets에만 넣습니다.

진행 순서: 1 → 2 → 3 → 4 → 5까지 하면 기본 기능(회원가입, 구독, 통계)이 배포됩니다. 6~8은 소셜 로그인과 메일 기능입니다.

---

## 0. 미리 준비

| 항목 | 용도 |
| --- | --- |
| JDK 21 | 로컬 빌드 (Gradle 8.14는 JDK 21~24에서 실행) |
| Node.js 22 | 프론트 빌드 |
| Docker Desktop | 로컬 DB, 테스트(Testcontainers) |
| Git | 저장소 push |

---

## 1. 비밀 토큰 2개 만들기

예약 작업 호출용 `JOB_TOKEN`, 알림 끄기 링크 서명용 `UNSUBSCRIBE_SECRET`을 무작위 문자열로 만듭니다. 터미널에서 두 번 실행해서 나온 값을 각각 메모장에 적어두세요.

```bash
# macOS / Linux / Git Bash
openssl rand -base64 32
```

```powershell
# Windows PowerShell
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }))
```

---

## 2. Neon (데이터베이스)

1. https://neon.tech 가입 → 프로젝트 생성
   - Postgres 버전: **17**
   - Region: **AWS Asia Pacific (Singapore)** (Render 서버와 같은 지역)
2. 프로젝트 대시보드에서 **Connect** → 연결 정보 확인
   - **Connection pooling은 끈 상태(직접 연결)** 의 주소를 씁니다. 호스트 이름에 `-pooler`가 없는 주소입니다.
     풀링 주소는 DB 스키마 변경(Flyway)과 충돌할 수 있습니다.
3. 접속 문자열은 이런 모양입니다.
   ```
   postgresql://사용자:비밀번호@ep-xxxx.ap-southeast-1.aws.neon.tech/neondb?sslmode=require
   ```
   Java는 이 형식을 그대로 못 씁니다. 아래 3개로 나눠서 Render에 넣습니다.
   | Render 환경변수 | 값 |
   | --- | --- |
   | `DB_URL` | `jdbc:postgresql://ep-xxxx.ap-southeast-1.aws.neon.tech/neondb?sslmode=require` |
   | `DB_USERNAME` | 사용자 |
   | `DB_PASSWORD` | 비밀번호 |

---

## 3. Render (서버)

1. https://render.com 가입 (GitHub 계정으로 가입하면 저장소 연결이 쉽습니다)
2. **New → Web Service** → GitHub 저장소 `Subby` 선택
3. 설정
   | 항목 | 값 |
   | --- | --- |
   | Name | `subby` (주소가 `https://subby-xxxx.onrender.com` 형태로 정해짐) |
   | Language / Runtime | **Docker** |
   | Branch | `main` |
   | Region | **Singapore** |
   | Instance Type | **Free** |
4. **Environment Variables**에 아래 값 입력 (6~8번 값은 나중에 추가해도 됩니다)
   | 키 | 값 |
   | --- | --- |
   | `SPRING_PROFILES_ACTIVE` | `prod` |
   | `APP_BASE_URL` | Render가 정해준 주소. 예: `https://subby-xxxx.onrender.com` (끝에 `/` 없이) |
   | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | 2번에서 만든 값 |
   | `JOB_TOKEN` | 1번에서 만든 값 |
   | `UNSUBSCRIBE_SECRET` | 1번에서 만든 값 |
   | `JAVA_TOOL_OPTIONS` | `-XX:MaxRAMPercentage=75` |
   `APP_BASE_URL`은 서비스를 만든 뒤에야 주소를 알 수 있으니, 만든 다음 **Environment** 탭에서 추가하고 다시 배포하면 됩니다.
5. **Create Web Service** → 첫 배포가 시작됩니다. 빌드에 5~10분 걸립니다.
6. 서비스의 **Settings** 탭
   - **Health Check Path**: `/actuator/health`
   - **Auto-Deploy**: **Off** (테스트를 통과한 코드만 GitHub Actions가 배포하도록)
   - **Deploy Hook**: 주소를 복사해둡니다 (4번에서 사용). 이 주소를 아는 사람은 누구나 배포를 일으킬 수 있으니 비밀로 다룹니다.
7. 확인: `https://subby-xxxx.onrender.com/actuator/health` 접속 → `{"status":"UP"}`

> 무료 서버는 15분 동안 요청이 없으면 잠듭니다. 잠든 뒤 첫 접속은 최대 1분 정도 걸립니다.

---

## 4. GitHub 설정

### Secrets
저장소 → **Settings → Secrets and variables → Actions → New repository secret**

| 이름 | 값 |
| --- | --- |
| `RENDER_DEPLOY_HOOK_URL` | 3-6에서 복사한 Deploy Hook 주소 |
| `APP_BASE_URL` | Render 주소 (끝에 `/` 없이) |
| `JOB_TOKEN` | 1번에서 만든 값 (Render와 같은 값) |

### 브랜치 보호
저장소 → **Settings → Branches → Add branch ruleset** (또는 Add rule)
- 대상 브랜치: `main`
- **Require a pull request before merging** 체크
- **Require status checks to pass** 체크 → 검사 이름 `build-and-test` 추가
  (CI가 한 번이라도 돌아야 목록에 나타납니다)

### 동작 확인
1. 기능 브랜치에서 일부러 실패하는 테스트를 넣어 PR을 엽니다 → 병합 버튼이 막히는지 확인
2. 정상 PR을 병합 → Actions 탭에서 `deploy` 작업이 실행되고 Render에서 새 배포가 시작되는지 확인
3. Actions 탭 → **Daily jobs** → **Run workflow**로 예약 작업을 수동 실행 → 성공하는지 확인

---

## 5. 배포 확인

- 회원가입 → 구독 등록 → 대시보드 확인
- 달러 구독 등록 → 대시보드 하단 환율 기준일이 표시되는지 확인
  (서버 시작 직후 백그라운드로 환율을 받아옵니다. "기본 환율"로 나오면 몇 분 뒤 새로고침)

---

## 6. Google 로그인

1. https://console.cloud.google.com → 프로젝트 생성
2. **Google Auth Platform** (또는 APIs & Services → OAuth consent screen) 설정
   - 앱 이름 `서비`, 지원 이메일 입력
   - Audience(대상): **External**
   - 범위(Scopes): `email`, `profile` (기본 범위만 쓰므로 Google 심사가 필요 없습니다)
   - 테스트 상태에서는 등록한 **테스트 사용자**만 로그인할 수 있습니다. 누구나 쓰게 하려면 **Publish app**(프로덕션 게시)
3. **Clients → Create client** (또는 Credentials → Create Credentials → OAuth client ID)
   - Application type: **Web application**
   - Authorized redirect URIs:
     ```
     https://subby-xxxx.onrender.com/login/oauth2/code/google
     http://localhost:8080/login/oauth2/code/google
     ```
4. 발급된 값을 Render 환경변수에 추가
   | 키 | 값 |
   | --- | --- |
   | `GOOGLE_CLIENT_ID` | 클라이언트 ID |
   | `GOOGLE_CLIENT_SECRET` | 클라이언트 보안 비밀번호 |

---

## 7. 카카오 로그인

1. https://developers.kakao.com → 로그인 → **내 애플리케이션 → 애플리케이션 추가하기** (앱 이름 `서비`)
2. **플랫폼**에 Web 사이트 도메인 등록
   ```
   https://subby-xxxx.onrender.com
   http://localhost:8080
   ```
3. **카카오 로그인 → 사용 설정 → 상태 ON**
4. **리다이렉트 URI** 등록
   ```
   https://subby-xxxx.onrender.com/login/oauth2/code/kakao
   http://localhost:8080/login/oauth2/code/kakao
   ```
5. **이메일 받기 설정 (개인 개발자 비즈 앱 전환)**
   - **앱 → 일반 → 비즈니스 정보 → 개인 개발자 비즈 앱 전환** (본인인증 필요)
   - 전환 후 **카카오 로그인 → 동의항목**
     - 닉네임: **필수 동의**
     - 카카오계정(이메일): **선택 동의**
   - 이메일을 거부한 사용자는 이메일 없는 계정이 되며 결제 알림·비밀번호 재설정을 쓸 수 없습니다 (화면에 안내됨)
   - **비즈 앱 전환을 안 하면** 이메일 동의항목을 켤 수 없고, 서비가 이메일을 요청하므로 카카오 로그인이 오류(KOE205)로 실패합니다
6. **앱 → 플랫폼 키 → REST API 키**에서 키와 **클라이언트 시크릿** 확인 (시크릿이 비활성 상태면 활성화)
7. Render 환경변수에 추가
   | 키 | 값 |
   | --- | --- |
   | `KAKAO_CLIENT_ID` | REST API 키 |
   | `KAKAO_CLIENT_SECRET` | 클라이언트 시크릿 |

> 카카오 개발자 콘솔은 메뉴 위치가 자주 바뀝니다. 메뉴가 안 보이면 [카카오 로그인 설정하기 공식 문서](https://developers.kakao.com/docs/latest/ko/kakaologin/prerequisite)를 확인하세요.

---

## 8. Brevo (메일 발송)

Render 무료 서버는 메일 SMTP 포트(25, 465, 587)가 막혀 있어서, 메일은 HTTPS API로 보냅니다.

1. https://www.brevo.com 가입 (무료: 하루 300통). 가입 후 계정 활성화 심사가 있을 수 있습니다.
2. **Senders, Domains & Dedicated IPs → Senders → Add a sender**
   - 보내는 사람 이메일: 본인 이메일 → 받은 인증 메일에서 확인
3. **SMTP & API → API Keys → Generate a new API key**
4. Render 환경변수에 추가
   | 키 | 값 |
   | --- | --- |
   | `BREVO_API_KEY` | 발급한 API 키 |
   | `MAIL_FROM` | 2번에서 인증한 이메일 |

> 자기 도메인 없이 개인 이메일(Gmail, 네이버 등)을 발신자로 쓰면 받는 쪽에서 **스팸함으로 분류될 가능성이 높습니다.**
> Brevo가 발신 주소를 자체 주소로 바꿔서 보낼 수도 있습니다. 화면에 "스팸함 확인" 안내를 넣어두었습니다.
> 도달률을 높이려면 도메인을 사서 Brevo에서 도메인 인증(SPF, DKIM)을 하면 됩니다.

확인: 비밀번호 찾기로 재설정 메일을 받아보세요. 구독을 오늘 결제일로 등록하고 Actions에서 Daily jobs를 수동 실행하면 결제 알림 메일도 확인할 수 있습니다.

---

## 문제 해결

| 증상 | 원인 / 해결 |
| --- | --- |
| Render 배포 로그에 DB 연결 실패 | `DB_URL`이 `jdbc:postgresql://`로 시작하는지, `?sslmode=require`가 있는지 확인 |
| 소셜 로그인 후 `redirect_uri_mismatch` (Google) / `KOE006` (카카오) | 콘솔에 등록한 리다이렉트 URI가 실제 주소와 한 글자라도 다름. `https`, 끝의 `/` 확인 |
| 카카오 `KOE205` | 이메일 동의항목 미설정. 7-5 진행 |
| 메일이 안 옴 | Render 로그에서 "BREVO_API_KEY 또는 MAIL_FROM이 없어" 경고 확인. 스팸함 확인 |
| Daily jobs가 403 | GitHub Secrets의 `JOB_TOKEN`과 Render의 `JOB_TOKEN`이 같은지 확인 |
| Daily jobs가 60일 뒤 멈춤 | GitHub 정책. Actions 탭에서 워크플로를 다시 Enable |
| 메모리 부족으로 재시작 반복 | `JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=75` 확인 |
