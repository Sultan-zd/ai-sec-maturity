# syntax=docker/dockerfile:1
# =====================================================================
# Image du frontend AIMA : build Vite, puis fichiers statiques servis par Nginx
#   docker build frontend
# =====================================================================

# ---- Étape 1 : build de production ----------------------------------
FROM node:25-slim AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci --no-audit --no-fund
COPY . .
RUN npm run build

# ---- Étape 2 : Nginx non-root (écoute sur 8080) ----------------------
FROM nginxinc/nginx-unprivileged:1.29-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=3s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/healthz || exit 1
