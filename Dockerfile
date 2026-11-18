FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd --system rotaleve && useradd --system --gid rotaleve --home-dir /app rotaleve \
    && mkdir -p /app/data && chown -R rotaleve:rotaleve /app
COPY --from=build --chown=rotaleve:rotaleve /build/target/rota-leve.jar /app/rota-leve.jar
USER rotaleve
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/rota-leve.jar"]
