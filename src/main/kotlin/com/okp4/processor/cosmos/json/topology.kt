package com.okp4.processor.cosmos.json

import com.google.protobuf.Any
import com.google.protobuf.util.JsonFormat
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Named
import org.apache.kafka.streams.kstream.Produced
import org.slf4j.LoggerFactory
import java.util.*

fun topology(props: Properties, typeRegistry: JsonFormat.TypeRegistry): Topology {
    val logger = LoggerFactory.getLogger("com.okp4.processor.cosmos.json.topology")
    val topicIn = requireNotNull(props.getProperty("topic.in")) {
        "Option 'topic.in' was not specified."
    }
    val topicOut = requireNotNull(props.getProperty("topic.out")) {
        "Option 'topic.out' was not specified."
    }
    val formatter =
        JsonFormat.printer()
            .usingTypeRegistry(typeRegistry)

    return StreamsBuilder()
        .apply {
            stream(topicIn, Consumed.with(Serdes.String(), Serdes.ByteArray()).withName("input"))
                .peek({ _, _ -> logger.info("Received a message") }, Named.`as`("log"))
                .mapValues({ v -> Any.parseFrom(v) }, Named.`as`("unmarshall"))
                .mapValues({ v -> formatter.print(v) }, Named.`as`("json-serialize"))
                .to(topicOut, Produced.with(Serdes.String(), Serdes.String()).withName("output"))
        }.build()
}
