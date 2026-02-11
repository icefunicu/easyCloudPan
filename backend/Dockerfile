FROM maven:3.9.11-eclipse-temurin-21-alpine AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache ffmpeg tzdata
WORKDIR /app
COPY --from=build /build/target/easypan-1.0.jar /app/app.jar
RUN mkdir -p /data/easypan /app/logs
EXPOSE 7090
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]
