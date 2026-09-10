# Daily Perfume 프런트엔드

전체 실행·환경변수·데이터 초기화·테스트 방법은 [루트 README](../README.md)를 참고하세요.

```bash
npm ci
npm run dev
npm test
npm run lint
npm run build
```

`predev`와 `prebuild`에서 Python/Pillow로 노트 썸네일을 생성하므로 루트의 Python 가상환경을 먼저 활성화하세요. `public`은 원본 보관용이며 `.generated-public`의 검증된 파일만 서비스합니다.
