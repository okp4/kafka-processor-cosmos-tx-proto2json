package com.okp4.processor.cosmos

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.protobuf.Any
import com.google.protobuf.GeneratedMessageV3
import com.hubspot.jackson.datatype.protobuf.ProtobufModule
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Named
import org.apache.kafka.streams.kstream.Produced
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Simple Kafka Stream Processor that consumes a protobuf message on a topic and returns a new message on another.
 */
fun topology(props: Properties): Topology {
    val logger = LoggerFactory.getLogger("com.okp4.processor.cosmos.topology")
    val topicIn = requireNotNull(props.getProperty("topic.in")) {
        "Option 'topic.in' was not specified."
    }
    val topicOut = requireNotNull(props.getProperty("topic.out")) {
        "Option 'topic.out' was not specified."
    }
    val mapper = ObjectMapper().registerModule(ProtobufModule())

    return StreamsBuilder()
        .apply {
            stream(topicIn, Consumed.with(Serdes.String(), Serdes.ByteArray()).withName("input"))
                .peek({ _, _ -> logger.info("Received a message") }, Named.`as`("log"))
                .map { k, v ->
                    try {

                        val wrappedObject: Any = Any.parseFrom(v)
                        val classType =
                            typeUrlToClassName(wrappedObject.typeUrl)
                                .let { Class.forName(it) as Class<*> }

                        @Suppress("UNCHECKED_CAST")
                        val protobufMessage: GeneratedMessageV3 =
                            wrappedObject.unpack(classType as Class<GeneratedMessageV3>)

                        val jsonMessage = mapper.writeValueAsString(protobufMessage)
                        KeyValue(k, jsonMessage)
                    } catch (ex: Exception) {
                        logger.error("Error decoding message", ex)
                        KeyValue(k, null)
                    }
                }
                .to(topicOut, Produced.with(Serdes.String(), Serdes.String()).withName("output"))
        }.build()
}

/**
 * Not ideal but working so far. Ex :
 * - cosmos.bank.v1beta1.$MsgSend -> cosmos.bank.v1beta1.Tx\$MsgSend
 * - okp4.okp4d.knowledge.Dataspace -> okp4.okp4d.knowledge.DataspaceOuterClass.Dataspace
 * - okp4.okp4d.knowledge.MsgBangDataspace -> okp4.okp4d.knowledge.Tx\$MsgBangDataspace
 */
fun typeUrlToClassName(typeUrl: String): String {
    return if (typeUrl.contains("okp4.okp4d.knowledge.Dataspace"))
        "okp4.okp4d.knowledge.DataspaceOuterClass\$Dataspace"
    else
        typeUrl.split('/')[1]
            .replace("cosmos.bank.v1beta1.", "cosmos.bank.v1beta1.Tx\$")
            .replace("okp4.okp4d.knowledge.", "okp4.okp4d.knowledge.Tx\$")
}
