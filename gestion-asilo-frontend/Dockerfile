# syntax=docker/dockerfile:1
#
# YS-174. Imagen del frontend en dos etapas: Node compila la aplicación y Caddy
# la sirve, termina HTTPS y reenvía /api al backend.

# ---- Etapa 1: compilación ----
FROM node:24-alpine AS construccion
WORKDIR /fuente

# Primero las dependencias: mientras el package-lock.json no cambie, Docker
# reutiliza esta capa y no las vuelve a instalar.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
# La aplicación y la API comparten dominio: el navegador pide /api/... al mismo
# servidor, y Caddy lo reenvía al backend (docs/fase-diseno.md, 2.5).
ENV VITE_API_URL=/api
RUN npm run build

# ---- Etapa 2: servidor web ----
FROM caddy:2-alpine
COPY Caddyfile /etc/caddy/Caddyfile
COPY --from=construccion /fuente/dist /srv
