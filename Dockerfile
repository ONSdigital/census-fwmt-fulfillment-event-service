FROM openjdk:11-jdk-slim
ARG jar

RUN groupadd -g 997 censusfulfilmentsvc && useradd -r -u 997 -g censusfulfilmentsvc censusfulfilmentsvc
USER censusfulfilmentsvc
COPY $jar /opt/censusfulfilmentsvc.jar
ENV JAVA_OPTS=""
CMD ["java",  "-jar", "/opt/censusfulfilmentsvc.jar"]
