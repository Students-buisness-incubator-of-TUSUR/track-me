# Build stage
FROM node:23-alpine AS builder
WORKDIR /app
RUN apk add --no-cache curl
COPY . .

RUN npm install && npm run build

EXPOSE 3000
CMD ["npm", "start"]