FROM node:24-alpine

WORKDIR /app

# Install dependencies first (better caching)
COPY package.json package-lock.json ./
RUN npm ci

# Copy source code
COPY . .

# Enable hot reload stability on Windows + Docker
ENV WATCHPACK_POLLING=true
ENV CHOKIDAR_USEPOLLING=true

EXPOSE 3000

# Run development server (NOT production)
CMD ["npm", "run", "dev"]