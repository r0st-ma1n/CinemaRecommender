# Бэкенд: Spring Boot + обученные модели Weka.
# Сборка в два этапа: в первом Maven собирает jar и обучает модели, во втором остаётся только JRE.

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Сначала только pom.xml: зависимости скачиваются отдельным слоем
# и не перекачиваются, пока pom.xml не изменится
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src src
COPY ml/*.csv ml/
# Модели в git не хранятся — обучаются из CSV (около 30 секунд), затем собирается jar
RUN mvn -q -B compile exec:java && mvn -q -B package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --no-create-home cinerec
COPY --from=build /app/target/cinerec-*.jar app.jar
COPY --from=build /app/ml/*.model ml/
USER cinerec
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
