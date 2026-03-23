# GitHub Actions에서 제공된 dist 폴더를 즉시 서빙합니다 (이중 빌드 방지 & 환경 변수 보존)
FROM nginx:stable-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
