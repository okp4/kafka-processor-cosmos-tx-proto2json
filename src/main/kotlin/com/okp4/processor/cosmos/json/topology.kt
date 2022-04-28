package com.okp4.processor.cosmos.json

import com.google.protobuf.util.JsonFormat
import cosmos.tx.v1beta1.TxOuterClass
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
    val isPrettyPrint = props.getProperty("formatter.prettyPrint").toBoolean()
    val formatter =
        JsonFormat.printer()
            .usingTypeRegistry(typeRegistry)
            .run {
                if (!isPrettyPrint) omittingInsignificantWhitespace() else this
            }

    return StreamsBuilder()
        .apply {
            stream(topicIn, Consumed.with(Serdes.String(), Serdes.ByteArray()).withName("input"))
                .peek({ _, _ -> logger.info("Received a message") }, Named.`as`("log"))
                .mapValues({ v ->
                    val txRaw = TxOuterClass.TxRaw.parseFrom(v)

                    TxOuterClass.Tx.newBuilder()
                        .addAllSignatures(txRaw.signaturesList)
                        .setBody(TxOuterClass.TxBody.parseFrom(txRaw.bodyBytes))
                        .setAuthInfo(TxOuterClass.AuthInfo.parseFrom(txRaw.authInfoBytes))
                        .build()
                }, Named.`as`("unmarshall"))
                .mapValues({ it ->
                    formatter.print(it)
                }, Named.`as`("serialize-json"))
                .to(topicOut, Produced.with(Serdes.String(), Serdes.String()).withName("output"))
        }.build()
}
