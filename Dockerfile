FROM openjdk:11-jdk-slim

COPY build/libs/kafka-processor-cosmos-tx-proto2json-*-standalone.jar /opt/kafka-processor-cosmos-tx-proto2json.jar

ENTRYPOINT ["java","-jar","/opt/kafka-processor-cosmos-tx-proto2json.jar"]

