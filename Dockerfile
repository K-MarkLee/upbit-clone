# build stage
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
COPY src src

RUN chmod +x gradlew
RUN ./gradlew bootJar --no-daemon -x test

# runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd -r -u 10001 appuser

COPY --from=build /workspace/build/libs/*.jar /app/app.jar

USER appuser

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:-} -jar /app/app.jar"]
