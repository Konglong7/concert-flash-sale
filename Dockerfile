# ==========================================
# Stage 1: Build Vue 3 Frontend
# ==========================================
FROM node:20-alpine AS frontend-builder
WORKDIR /app/frontend

COPY frontend/package*.json ./
RUN npm install

COPY frontend/ ./
RUN npm run build

# ==========================================
# Stage 2: Build Spring Boot 3 Backend
# ==========================================
FROM maven:3.9-eclipse-temurin-17-alpine AS backend-builder
WORKDIR /app

COPY backend/pom.xml ./backend/
RUN mvn -f ./backend/pom.xml dependency:go-offline -B || true

COPY backend/src ./backend/src
COPY backend/sql ./backend/sql

# Copy frontend dist to backend resources/static for single-container SPA serving
COPY --from=frontend-builder /app/frontend/dist ./backend/src/main/resources/static

RUN mvn -f ./backend/pom.xml clean package -DskipTests -B

# ==========================================
# Stage 3: Lightweight Production Runtime
# ==========================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# JVM flags optimized for 512MB RAM containers (Render Free Tier)
ENV JAVA_OPTS="-Xms128m -Xmx300m -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m -Xss512k -Djava.security.egd=file:/dev/./urandom"
ENV PORT=8080

COPY --from=backend-builder /app/backend/target/flash-sale-backend-1.0.0.jar app.jar
COPY backend/sql ./sql

EXPOSE ${PORT}

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --server.port=${PORT}"]
