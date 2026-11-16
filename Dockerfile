# Etapa 1 — compilar a API com Java 17 e Maven
FROM eclipse-temurin:17-jdk AS build
WORKDIR /build

RUN apt-get update && apt-get install -y --no-install-recommends curl unzip \
    && rm -rf /var/lib/apt/lists/*

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/

RUN sh ./mvnw -B -DskipTests package

# Etapa 2 — executar a API com Java 17
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /build/target/projetoTCC-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]