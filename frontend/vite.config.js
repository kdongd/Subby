import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// 빌드 결과는 Spring Boot의 정적 파일 폴더로 나간다. 서버 하나가 화면과 API를 같은 주소로 내려준다.
// 개발 중(npm run dev)에는 API 요청을 8080 포트의 Spring Boot로 넘긴다.
const backend = 'http://localhost:8080';

export default defineConfig({
  plugins: [react()],
  build: {
    outDir: '../src/main/resources/static',
    emptyOutDir: true,
    chunkSizeWarningLimit: 800,
  },
  server: {
    port: 5173,
    proxy: {
      '/api': backend,
      '/oauth2': backend,
      '/login/oauth2': backend,
      '/internal': backend,
    },
  },
});
