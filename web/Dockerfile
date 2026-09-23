# ---------- build
FROM node:22.23.2-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
# Same-origin API: nginx proxies /api to the backend container.
RUN printf '{\n  "apiUrl": ""\n}\n' > public/config.json && npx ng build --configuration production

# ---------- runtime (non-root nginx listening on 8080)
FROM nginxinc/nginx-unprivileged:1.30.5-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/web/browser /usr/share/nginx/html
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --retries=3 CMD wget -qO- http://127.0.0.1:8080/ > /dev/null || exit 1
