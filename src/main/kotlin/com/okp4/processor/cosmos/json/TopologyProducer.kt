package com.okp4.processor.cosmos.json

import com.google.protobuf.util.JsonFormat
import cosmos.tx.v1beta1.TxOuterClass
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Named
import org.apache.kafka.streams.kstream.Produced
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.slf4j.LoggerFactory
import java.util.*
import javax.enterprise.context.ApplicationScoped
import javax.enterprise.inject.Produces

@ApplicationScoped
class TopologyProducer {
    @field:ConfigProperty(name = "topic.in", defaultValue = "topic.in")
    lateinit var topicIn: String
    @field:ConfigProperty(name = "topic.out", defaultValue = "topic.out")
    lateinit var topicOut: String
    @field:ConfigProperty(name = "topic.error", defaultValue = "")
    var topicError: String? = null
    @field:ConfigProperty(name = "prettyprint", defaultValue = "false")
    var prettyPrint = false

    @Produces
    fun buildTopology(): Topology {
        val logger = LoggerFactory.getLogger(TopologyProducer::class.java)

        val formatter =
            JsonFormat.printer()
                .usingTypeRegistry(ProtoTypeRegistry.protoTypeRegistry)
                .run {
                    if (!prettyPrint) omittingInsignificantWhitespace() else this
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
}
