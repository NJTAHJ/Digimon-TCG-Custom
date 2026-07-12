# Stage 1: Compile the Java code inside a Maven container
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY backend /app/backend
RUN mvn -B package --file backend/pom.xml -DskipTests

# Stage 2: Run the application in a lightweight runtime container
FROM eclipse-temurin:21
ENV ENVIRONMENT=prod
LABEL maintainer="WE-Kaito"
WORKDIR /app
COPY --from=build /app/backend/target/digimon-tcg-sim.jar /app/digimon-tcg-sim.jar
CMD ["sh", "-c", "java -DServer.port=$PORT -jar /app/digimon-tcg-sim.jar"]