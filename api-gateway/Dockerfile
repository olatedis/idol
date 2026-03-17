FROM amazoncorretto:21-al2023-jdk AS builder
RUN dnf install -y findutils
ENV GRADLE_OPTS="-Dorg.gradle.jvmargs=-Xmx512m -XX:MaxMetaspaceSize=256m"
WORKDIR /source
COPY gradlew .
RUN chmod +x gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN ./gradlew dependencies --no-daemon

COPY src src
RUN ./gradlew bootJar -x test --no-daemon

# 2단계: 실행
FROM amazoncorretto:21-al2023-jdk
WORKDIR /app
COPY --from=builder /source/build/libs/*.jar app.jar

# 도커 프로파일 활성화
ENV SPRING_PROFILES_ACTIVE=docker

ENTRYPOINT ["java", "-jar", "app.jar"]
