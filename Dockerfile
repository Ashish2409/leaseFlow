FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Add non-root user for security
RUN addgroup -S leaseflow && adduser -S leaseflow -G leaseflow

COPY build/libs/*.jar app.jar

RUN chown leaseflow:leaseflow app.jar
USER leaseflow

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
