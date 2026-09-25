# syntax=docker/dockerfile:1
# =============================================================================
#  Frontend React — build Vite puis service statique par nginx
# =============================================================================

FROM node:22-alpine AS build
WORKDIR /build

# npm ci exige package-lock.json : la construction est reproductible.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# -----------------------------------------------------------------------------
FROM nginx:1.27-alpine AS execution

# nginx:alpine fournit deja l'utilisateur nginx ; on lui donne les droits sur les
# repertoires qu'il doit ecrire, puis on abandonne root.
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist /usr/share/nginx/html

RUN touch /var/run/nginx.pid \
 && chown -R nginx:nginx /var/run/nginx.pid /var/cache/nginx /usr/share/nginx/html
USER nginx:nginx

EXPOSE 8081

# 127.0.0.1 et non localhost : dans le conteneur, localhost resout d'abord en
# IPv6 (::1), or nginx n'ecoute qu'en IPv4. La sonde echouait donc alors que le
# service repondait parfaitement de l'exterieur.
HEALTHCHECK --interval=15s --timeout=3s --start-period=5s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8081/ >/dev/null || exit 1
