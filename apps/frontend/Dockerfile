# Build stage
FROM node:23-alpine AS builder
WORKDIR /app
RUN apk add --no-cache curl
COPY . .

RUN npm install

ARG REACT_APP_ENV
RUN cp .env.${REACT_APP_ENV} .env \
  && npm run build

EXPOSE 3000
CMD ["npm", "start"]