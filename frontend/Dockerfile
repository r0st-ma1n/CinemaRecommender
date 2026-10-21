# Фронтенд: сборка Vite, раздача через nginx. Запросы /api nginx передаёт бэкенду (см. nginx.conf).

FROM node:24-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
# В контейнере фронтенд работает с настоящим бэкендом, а не с моками
ENV VITE_API_MODE=real
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
