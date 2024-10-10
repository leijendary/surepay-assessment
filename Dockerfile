FROM azul/zulu-openjdk-alpine:21 AS build
# Download the Gradle distribution.
COPY gradlew .
COPY gradle gradle
RUN --mount=type=cache,target=/root/.gradle ./gradlew --version
# Download dependencies.
COPY settings.gradle .
COPY build.gradle .
RUN --mount=type=cache,target=/root/.gradle ./gradlew dependencies
# Add source code.
COPY src src
# Run Gradle compiler.
RUN --mount=type=cache,target=/root/.gradle ./gradlew bootJar -i

FROM azul/zulu-openjdk-alpine:21-jre-headless-latest
COPY --from=build build/libs/*.jar application.jar
ENTRYPOINT ["java", "-jar", "application.jar"]
