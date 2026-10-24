FROM node:22-bookworm-slim
ENV NODE_ENV=production
WORKDIR /app
COPY server/package*.json ./
RUN npm ci --omit=dev
COPY server/src ./src
USER node
EXPOSE 8080
CMD ["node", "src/index.js"]
