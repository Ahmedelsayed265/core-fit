# ============================
#       Build Stage
# ============================
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper & config files
COPY mvnw pom.xml ./
COPY .mvn .mvn

# Fix permissions
RUN chmod +x mvnw

# ----------------------------
# Step 1: Download dependencies only
# ----------------------------
# This creates cache layer → faster builds
RUN ./mvnw -B -DskipTests dependency:resolve dependency:resolve-plugins

# ----------------------------
# Step 2: Copy source code
# ----------------------------
COPY src ./src

# ----------------------------
# Step 3: Build Application
# ----------------------------
RUN ./mvnw -B -DskipTests package


# ============================
#       Runtime Stage
# ============================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy built JAR
COPY --from=builder /app/target/core-fit-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8000

ENTRYPOINT ["java", "-jar", "app.jar"]

