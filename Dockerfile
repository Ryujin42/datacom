# ---- Build stage ------------------------------------------------------
# Dependencies are resolved in their own layer, before the sources are copied,
# so that editing a .java file never invalidates the Maven dependency cache (ECO-15).
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package -DskipTests

# ---- Runtime stage ------------------------------------------------------
# JRE only, no compiler/build tooling shipped in the image that runs in production (ECO-14).
FROM eclipse-temurin:25-jre-alpine

RUN addgroup -S datacom && adduser -S datacom -G datacom
WORKDIR /app
COPY --from=build /build/target/datacom.jar app.jar
RUN chown datacom:datacom app.jar
USER datacom

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
