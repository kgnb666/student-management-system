# 构建阶段：用 Maven 打包可执行 JAR
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml ./
COPY src ./src
RUN mvn -B -DskipTests package

# 运行阶段：只保留 JRE 与 JAR
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/target/student-score-1.0.0.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
