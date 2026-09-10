# Daily Perfume

React + Spring Boot + FastAPI로 구성된 향수 검색 서비스입니다. AI는 DB에 존재하는 제품 ID와 추천 이유만 선택합니다. 이름·브랜드·이미지·향 노트는 DB에서 가져오며, 원본에 탑/미들/베이스가 있을 때만 구분해서 표시합니다.

## 개발 환경

- Java 17, Node.js 22, Python 3.12, MySQL 8
- 최초 실행에는 npm/Maven/PyPI 및 Hugging Face 모델 다운로드 연결이 필요합니다.
- 원본 노트 이미지 약 545 MB를 포함하므로 전체 저장소를 받아 주세요. 빌드 시 Pillow가 320px WebP 썸네일을 만들고 프런트·백엔드에 같은 파일과 매핑을 배포합니다. 원본 HTML·손상 이미지·0바이트 파일은 배포하지 않습니다.

## 최초 실행

저장소 루트에서 실행합니다.

```bash
python3 -m venv .venv
source .venv/bin/activate
python -m pip install -r Fast-API/requirements.txt
cp Fast-API/.env.example Fast-API/.env
```

`Fast-API/.env`에 본인의 MySQL 계정과 DB 이름을 넣습니다. MySQL에서 DB와 최소 권한의 앱 계정을 먼저 생성해야 합니다. 비밀 값이 들어 있는 `.env`는 Git에 추가하지 않습니다.

Spring도 같은 DB 설정을 사용하도록 해당 셸에 `DB_USER`, `DB_PASSWORD`, `DB_URL`을 설정합니다. `DB_URL` 예시는 `jdbc:mysql://localhost:3306/perfume_db`입니다. AI 설명과 상상 속 조향 제안을 쓰려면 `GEMINI_API_KEY`도 설정합니다. 키가 없으면 FAISS 검색 결과만 반환합니다.

```bash
cd recommendation
./gradlew bootRun
```

개발 기본값인 `ddl-auto=update`가 향수 테이블을 준비합니다. 운영에서는 검토된 DB 마이그레이션을 적용한 뒤 `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`를 사용하세요.

다른 터미널에서 저장소 루트로 이동하고 같은 가상환경을 활성화한 후 데이터를 입력합니다.

```bash
python Fast-API/import_csv.py
python recommendation/import_notes.py
python Fast-API/build_faiss.py
python -m uvicorn search_engine:app --app-dir Fast-API --host 127.0.0.1 --port 8000
```

- CSV의 배열·피라미드 JSON 및 `null`을 처리합니다. 제품명이 없는 원본 CSV 1219행은 경고 후 제외하며 임의의 이름을 만들지 않습니다.
- 제품은 브랜드·이름 기준으로 기존 ID를 유지해 갱신합니다. 기존 DB에 동일한 브랜드·이름의 중복 행이 있으면 자동 삭제하지 않고 중단합니다. 해당 제품의 ID 참조와 구분 여부를 확인해 정리한 뒤 재실행하세요.
- 번역은 파일을 먼저 검증하고 기존 테이블을 삭제하지 않는 upsert로 입력합니다. 변경된 번역은 최대 5분 후 반영됩니다.
- 기존 `perfumes.index`/`perfumes_meta.pkl`은 사용하지 않습니다. 새 생성 명령은 `artifacts/CURRENT`와 버전별 인덱스·JSON 메타데이터를 만듭니다. DB 변경 뒤 재생성하고 FastAPI를 재시작해야 합니다.
- `INDEX_DIR`를 지정하면 인덱스 위치를 변경할 수 있습니다. 기존 버전 파일은 진행 중인 읽기와 충돌하지 않도록 자동 삭제하지 않습니다.

프런트엔드는 다른 터미널에서 실행합니다. 이 셸에서도 Pillow를 설치한 가상환경을 활성화해야 합니다.

```bash
cd perfume-frontend
npm ci
npm run dev
```

`http://localhost:5173`으로 접속합니다. 개발 Vite가 `/api` 요청을 Spring의 8080 포트로 전달합니다.

## API와 배포 설정

- `POST /api/perfumes/search`: `{ "keyword": "봄 장미 향수", "excludedNotes": ["Musk"] }`
- 제외 노트는 영문 DB 노트의 대소문자 무관 완전 일치 필터입니다. 자유 문장에 포함된 모든 부정 표현까지 확정적으로 해석하는 기능은 아닙니다. 온보딩의 달콤함·무거움 옵션은 화면에 명시된 노트 목록을 제외하며 필터를 해제할 수 있습니다.
- `GET /api/perfumes/{id}`와 `/{id}/recommendations`는 검색과 같은 제품 DTO를 반환합니다.
- 페이지 크기는 1~100, 검색어는 최대 1000자입니다. 검색은 인스턴스당 기본 30회/분으로 제한되며 429에 `Retry-After`를 반환합니다. 여러 인스턴스로 운영하면 게이트웨이의 공유 쿼터도 설정해야 합니다.
- `VECTOR_SEARCH_URL`: 컨테이너/서버 환경의 FastAPI 내부 주소. FastAPI 포트는 외부에 직접 공개하지 않습니다.
- `ALLOWED_ORIGINS`: 쉼표로 구분한 실제 프런트 주소.
- `AI_REQUESTS_PER_MINUTE`, `ai.connect-timeout-ms`, `ai.read-timeout-ms`: 호출 한도와 타임아웃 설정.
- 프런트 `VITE_API_BASE_URL`: 다른 오리진 API를 사용할 때 빌드 전에 지정합니다. 비어 있으면 운영 웹 서버에서 `/api`를 Spring으로 프록시해야 합니다. Vite 개발 프록시는 정적 배포에 포함되지 않습니다.
- 원본에 없는 향 노트나 노트 분류를 AI가 제품 정보에 추가할 수 없습니다. 상상 속 조향 제안은 선택된 제품의 노트에 포함되는지 검증한 뒤 별도로 표시합니다.
- AI 응답 오류·빈 응답·외부 오류가 발생하면 DB로 검증된 FAISS 후보로 대체합니다. 외부 오류 본문과 API 키는 클라이언트에 반환하지 않습니다.

## 검증

```bash
python -m unittest discover -s Fast-API/tests -v
cd perfume-frontend
npm test
npm run lint
npm run build
cd ../recommendation
./gradlew test bootJar
```

GitHub Actions는 위 검증에 더해 Chromium에서 손상된 저장 데이터, 같은 이름의 서로 다른 제품 찜, 찜 목록의 상세 창, 노트 표시, 모바일 가로 넘침을 확인합니다. Java 테스트는 H2와 가짜 외부 API를 사용하므로 운영 DB·API 키를 사용하지 않습니다.
