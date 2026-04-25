#!/usr/bin/env bash
# deploy-aliyun.sh — Pull latest images from Aliyun ACR and restart application services.
# Usage: bash scripts/deploy-aliyun.sh [IMAGE_TAG]
#   IMAGE_TAG  Docker image tag to deploy (default: main)
#
# Prerequisites on the server:
#   - Docker Engine installed
#   - Docker Compose plugin (docker compose) or standalone docker-compose
#   - Aliyun ACR credentials (ALIYUN_USERNAME / ALIYUN_PASSWORD) available as
#     environment variables or entered interactively when prompted

set -euo pipefail

REGISTRY="crpi-cbkazoyqsrh7o1rk.cn-shanghai.personal.cr.aliyuncs.com"
IMAGE_TAG="${1:-main}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "==> Deploying classroom (tag: ${IMAGE_TAG}) from Aliyun ACR"

# ── 1. Login to Aliyun Container Registry ──────────────────────────────────
if [[ -z "${ALIYUN_USERNAME:-}" || -z "${ALIYUN_PASSWORD:-}" ]]; then
  echo "Aliyun ACR credentials not found in environment."
  read -rp "Aliyun ACR username: " ALIYUN_USERNAME
  read -rsp "Aliyun ACR password: " ALIYUN_PASSWORD
  echo
fi

echo "==> Logging in to ${REGISTRY}"
# --password-stdin avoids the password appearing in shell history or process listings
echo "${ALIYUN_PASSWORD}" | docker login "${REGISTRY}" \
  --username "${ALIYUN_USERNAME}" --password-stdin

# ── 2. Pull latest images ───────────────────────────────────────────────────
echo "==> Pulling backend image (tag: ${IMAGE_TAG})"
docker pull "${REGISTRY}/classrooom/class_github_01:${IMAGE_TAG}"

echo "==> Pulling frontend image (tag: ${IMAGE_TAG})"
docker pull "${REGISTRY}/classrooom/class_github_frontend:${IMAGE_TAG}"

# ── 3. Start / restart services ────────────────────────────────────────────
cd "${PROJECT_DIR}"

echo "==> Starting infrastructure services (MySQL / Redis / MongoDB)"
docker compose -f docker-compose.yml up -d

echo "==> Starting application services"
IMAGE_TAG="${IMAGE_TAG}" docker compose -f docker-compose.aliyun.yml up -d --remove-orphans

echo "==> Deployment complete."
echo "    Frontend: http://<server-ip>"
echo "    Backend:  http://<server-ip>:8080"
