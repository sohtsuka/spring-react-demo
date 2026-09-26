# 本番構成の SPA イメージ。dist をビルドし、nginx で配信する。

FROM docker.io/library/node:24-alpine AS build
WORKDIR /build
ENV CI=true

# packageManager の pnpm を corepack で有効にする
RUN corepack enable

COPY package.json pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile

COPY . .
RUN pnpm build

FROM docker.io/library/nginx:alpine
# 検査用の自己署名証明書。本番では正規の証明書をマウントして置き換える。
RUN apk add --no-cache openssl \
    && mkdir -p /etc/nginx/tls \
    && openssl req -x509 -nodes -newkey rsa:2048 -days 365 \
    -keyout /etc/nginx/tls/server.key \
    -out /etc/nginx/tls/server.crt \
    -subj "/CN=localhost" \
    -addext "subjectAltName=DNS:localhost,IP:127.0.0.1" \
    && chmod 600 /etc/nginx/tls/server.key

# entrypoint が起動時に envsubst で展開し /etc/nginx/conf.d/default.conf に出力する
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /build/dist /usr/share/nginx/html

EXPOSE 80 443
