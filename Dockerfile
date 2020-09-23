FROM openjdk:11-jdk-slim
ARG jar

RUN groupadd -g 997 censusjobsvc && useradd -r -u 997 -g censusjobsvc censusjobsvc
USER censusjobsvc
COPY $jar /opt/census-fulfillment-events-svc.jar

CMD ["java",  "-jar", "/opt/census-fulfillment-events-svc.jar"]
