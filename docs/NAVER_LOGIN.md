# 네이버 로그인 연결 — Spring Boot + 기존 React 웹

## 적용 범위와 현재 상태

이 브랜치는 GitHub의 `recommendation` Spring Boot 서버와 `perfume-frontend` React 화면을 연결합니다. 네이버 로그인, 내부 회원 번호, 계정별 저장/해제/조회, 로그아웃을 추가합니다. 로그인 전에도 기존 조회·추천을 이용할 수 있습니다.

**현재 ChatGPT Sites 화면에 네이버 로그인을 배포한 것은 아닙니다.** Sites의 6문항 설문과 Worker 추천 서버는 이 GitHub의 React/Java 코드와 서로 다른 구현입니다. Sites를 네이버 로그인으로 우회하지 않습니다. 기존 Sites 저장 기록과 브라우저 localStorage 찜 기록도 자동 이전하지 않습니다. 운영 도메인 확정 후 최신 Sites 화면 이식과 추천 API 계약 통합은 별도 작업입니다.

이 환경에서는 Gradle 배포본 다운로드가 `Network is unreachable`로 실패하여 Java 컴파일/통합 테스트는 실행하지 못했습니다. 포함된 테스트는 실행 결과가 아닌 검증용 코드입니다. 실제 네이버 인증은 키 발급 후 확인해야 합니다. 바로 운영 출시할 수 있다고 검증한 상태가 아닙니다.

## 1. 준비

- Java 17 이상, Node 22.12 이상, MySQL 8, 기존 `perfumes` 테이블과 향수 데이터.
- 기존 추천 API는 Python 검색 서버와 Gemini 설정을 사용합니다. 이번 로그인 작업에서 추천 모델을 변경하지 않았습니다. 기존 키 설정은 그대로 유지하세요.
- 기존 DB 백업 후 `recommendation/db/001-member-login.sql`을 한 번 적용합니다. 테이블이 이미 있으면 재실행하지 마세요.
- 회원 정보는 네이버의 앱별 사용자 식별자와 내부 UUID만 저장합니다. 같은 이메일을 기준으로 다른 계정을 합치지 않습니다.

## 2. 네이버 개발자센터 등록

https://developers.naver.com/apps/#/register 에서 네이버 로그인 애플리케이션을 등록합니다.

로컬 개발:
- 서비스 URL: `http://localhost:5173`
- Callback URL: `http://localhost:5173/login/oauth2/code/naver`
- 네이버 개발 중 설정에서 본인 테스트 계정을 등록합니다.
- 이메일, 전화번호, 생일, 성별 등을 요청하지 않는 최소 정보 구성으로 시작합니다.

운영 전에는 실제 HTTPS 도메인으로 서비스 URL/Callback URL을 등록하고 검수 절차를 확인합니다. Callback URL은 환경변수와 정확히 같아야 합니다. 로그인 시작과 복귀는 같은 도메인을 사용해야 합니다.

## 3. 서버 설정

`recommendation/src/main/resources/application.properties`는 Git에 넣지 않는 기존 로컬 설정 파일입니다. 없으면 생성하고 기존 DB/추천 설정과 함께 다음 내용을 넣습니다. 키 자체를 이 문서나 소스에 넣지 마세요.

```properties
spring.config.import=classpath:application-auth.properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=validate
gemini.api.key=${GEMINI_API_KEY:}
```

Mac 터미널의 **서버를 실행할 창**에서 설정합니다(zsh).

```sh
export DB_URL='jdbc:mysql://localhost:3306/기존DB이름?serverTimezone=Asia/Seoul'
export DB_USER='DB사용자'
read -s 'DB_PASSWORD?DB 비밀번호: '; echo; export DB_PASSWORD
export NAVER_LOGIN_ENABLED=true
read 'NAVER_CLIENT_ID?네이버 Client ID: '; export NAVER_CLIENT_ID
read -s 'NAVER_CLIENT_SECRET?네이버 Client Secret: '; echo; export NAVER_CLIENT_SECRET
export NAVER_REDIRECT_URI='http://localhost:5173/login/oauth2/code/naver'
export SESSION_COOKIE_SECURE=false
cd recommendation
bash gradlew test
bash gradlew bootRun
```

키는 채팅에 보내지 마세요. `SESSION_COOKIE_SECURE=false`는 로컬 HTTP 개발에만 사용합니다. 키가 없으면 `NAVER_LOGIN_ENABLED=false`로 시작할 수 있고 로그인 버튼은 준비 중으로 표시됩니다.

## 4. 화면 실행

새 터미널에서 저장소 루트 기준:

```sh
cd perfume-frontend
npm ci
npm run dev -- --host localhost --port 5173 --strictPort
```

`http://localhost:5173`에 접속합니다. Vite가 `/api`, `/oauth2`, `/login/oauth2`를 Spring Boot 8080으로 전달합니다. 8080에서 직접 로그인하거나 localhost와 127.0.0.1을 섞지 마세요. 화면의 네이버 로그인 → 네이버 동의 → 원래 화면 복귀 → 향수 저장을 확인합니다.

## 5. 검증 순서

1. 로그인하지 않고 향수 조회가 가능하고, 저장 API는 401인지 확인합니다.
2. 네이버 로그인 취소/실패 후 재시도가 가능한지 확인합니다.
3. 로그인 후 하트 저장 → 새로고침 → 같은 계정에 목록이 남는지 확인합니다.
4. 다른 계정에서는 이전 회원의 저장 목록이 보이지 않아야 합니다.
5. CSRF 토큰 없는 저장·삭제·로그아웃은 차단되어야 합니다.
6. 저장할 향수가 AI 결과에 ID 없이 포함되면 이름과 브랜드가 정확히 하나의 DB 제품과 일치할 때만 저장합니다. 모호하면 저장을 거절합니다.
7. 로그아웃 후 목록이 사라지고 저장 API가 401인지 확인합니다.

## 6. 운영 배포

별도 Spring Boot 호스팅/HTTPS 도메인이 필요합니다. 이번 작업에서는 유료 자원을 생성하거나 기존 사이트를 공개하지 않았습니다.

가장 작은 운영 구성은 React 정적 파일을 Spring Boot에 함께 넣는 단일 서버입니다.

```sh
cd perfume-frontend
npm ci
npm run build
cd ..
mkdir -p recommendation/src/main/resources/static
cp -R perfume-frontend/dist/. recommendation/src/main/resources/static/
cd recommendation
bash gradlew bootJar
java -jar build/libs/recommendation-0.0.1-SNAPSHOT.jar
```

기존 향료 이미지는 삭제하지 마세요. 운영 환경에는 DB 변수/네이버 비밀 값/기존 추천 설정을 비밀 환경변수로 등록합니다.
- `NAVER_REDIRECT_URI=https://실제도메인/login/oauth2/code/naver`
- `SESSION_COOKIE_SECURE=true`
- `NAVER_LOGIN_ENABLED=true`
- TLS 프록시 뒤에서는 전달 헤더 신뢰 범위를 운영 환경에 맞게 제한하세요.

세션은 현재 단일 서버 메모리에 보관하므로 재시작하면 재로그인해야 합니다. 서버를 여러 대로 늘리기 전에 Spring Session/Redis 같은 공유 세션 구성이 필요합니다. 회원 탈퇴/네이버 연결 해제, 운영 약관·개인정보 안내, 공개 추천 API의 사용량 제한은 일반 사용자 공개 전에 추가해야 합니다.

## API

| 경로 | 역할 |
| --- | --- |
| GET /api/auth/me | 로그인 여부·로그인 연결 준비 여부 |
| GET /api/auth/csrf | 현재 세션의 쓰기 요청용 CSRF 토큰 |
| GET /oauth2/authorization/naver | 네이버 로그인 시작 |
| GET /login/oauth2/code/naver | Spring Security가 처리하는 인증 복귀 |
| POST /api/auth/logout | 사이트 세션 종료(네이버 자체 로그아웃 아님) |
| GET /api/member/saved | 현재 회원의 저장한 향수 |
| PUT /api/member/saved/{id} | 중복 없이 저장 |
| DELETE /api/member/saved/{id} | 현재 회원의 저장만 해제 |

공식 참고: https://developers.naver.com/docs/login/devguide · https://developers.naver.com/docs/login/profile/profile.md · https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html
