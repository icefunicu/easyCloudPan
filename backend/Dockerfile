FROM maven:3.9.11-eclipse-temurin-21-alpine AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache ffmpeg tzdata && \
    addgroup -S easypan && adduser -S easypan -G easypan 
WORKDIR /app
COPY --from=build /build/target/easypan-1.0.jar /app/app.jar
RUN mkdir -p /data/easypan /app/logs && \
    chown -R easypan:easypan /app /data/easypan

# 配置 JVM 内存与垃圾回收器 (含容器感知与ZGC)
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -XX:+UseZGC -XX:+ZGenerational"

USER easypan
EXPOSE 7090
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar"]
