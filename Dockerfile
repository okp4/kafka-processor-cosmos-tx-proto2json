FROM openjdk:11-jdk-slim

COPY build/libs/kafka-processor-*-standalone.jar /opt/kafka-processor.jar
