
# syntax=docker/dockerfile:1

# ---------- Build stage ----------
FROM node:18-alpine AS build
WORKDIR /app

COPY frontend/package*.json ./
RUN npm ci

COPY frontend ./
RUN npm run build

# ---------- Runtime stage ----------
FROM nginx:1.27-alpine

# SPA routing + optional reverse proxy can be set in nginx.conf
COPY docker/frontend/nginx.conf /etc/nginx/conf.d/default.conf

COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80



