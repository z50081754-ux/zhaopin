FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN addgroup --system xw && adduser --system --ingroup xw xw
COPY --from=build /app/target/recruitment-api-0.0.1-SNAPSHOT.jar app.jar
RUN mkdir -p /app/data && chown -R xw:xw /app
USER xw
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
