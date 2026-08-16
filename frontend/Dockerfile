# syntax=docker/dockerfile:1
FROM node:22-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
# --ignore-scripts: @playwright/test's postinstall tries to download a
# browser binary, which this image never needs (e2e specs don't run here)
# and which stalls indefinitely on networks that block/throttle
# cdn.playwright.dev — see CLAUDE.md's Playwright gotcha.
RUN npm ci --ignore-scripts

FROM node:22-alpine AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
# NEXT_PUBLIC_* values are baked into the client bundle at build time, not
# read at container-start — must be supplied as build args, not just env.
ARG NEXT_PUBLIC_API_GATEWAY_URL
ARG NEXT_PUBLIC_SITE_URL
ARG NEXT_PUBLIC_GRAFANA_URL
ENV NEXT_PUBLIC_API_GATEWAY_URL=$NEXT_PUBLIC_API_GATEWAY_URL
ENV NEXT_PUBLIC_SITE_URL=$NEXT_PUBLIC_SITE_URL
ENV NEXT_PUBLIC_GRAFANA_URL=$NEXT_PUBLIC_GRAFANA_URL
RUN npm run build

FROM node:22-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production
# output: "standalone" (next.config.ts) traces only the node_modules this
# app needs into .next/standalone — public/ and .next/static aren't part of
# that trace and have to be copied in separately.
COPY --from=builder /app/public ./public
COPY --from=builder /app/.next/standalone ./
COPY --from=builder /app/.next/static ./.next/static

EXPOSE 3000
ENV PORT=3000 HOSTNAME=0.0.0.0
CMD ["node", "server.js"]
