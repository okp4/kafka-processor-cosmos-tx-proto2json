package com.okp4.processor.cosmos.json

import com.google.protobuf.util.JsonFormat
import cosmos.tx.v1beta1.TxOuterClass
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Branched
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Named
import org.apache.kafka.streams.kstream.Produced
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.slf4j.LoggerFactory
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

    @field:ConfigProperty(name = "formatter.prettyPrint", defaultValue = "false")
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
                        Pair(
                            v,
                            kotlin.runCatching {
                                TxOuterClass.TxRaw.parseFrom(v).run {
                                    TxOuterClass.Tx.newBuilder()
                                        .addAllSignatures(this.signaturesList)
                                        .setBody(TxOuterClass.TxBody.parseFrom(this.bodyBytes))
                                        .setAuthInfo(TxOuterClass.AuthInfo.parseFrom(this.authInfoBytes))
                                        .build()
                                }
                            }
                        )
                    }, Named.`as`("unmarshall"))
                    .split()
                    .branch(
                        { _, v -> v.second.isFailure },
                        Branched.withConsumer { stream ->
                            stream.peek(
                                { k, v ->
                                    v.second.onFailure {
                                        logger.warn("Unmarshalling failed for tx with key <$k>; ${it.message}", it)
                                    }
                                },
                                Named.`as`("log-unmarshalling-failure")
                            )
                                .mapValues({ pair -> pair.first }, Named.`as`("extract-tx-bytearray"))
                                .apply {
                                    if (!topicError.isNullOrEmpty()) {
                                        logger.info("Failed tx will be sent to the topic $topicError")
                                        to(
                                            topicError, Produced.with(Serdes.String(), Serdes.ByteArray()).withName("error")
                                        )
                                    }
                                }
                        }
                    ).defaultBranch(
                        Branched.withConsumer { stream ->
                            stream.mapValues(
                                { v ->
                                    v.second.getOrThrow()
                                }, Named.`as`("extract-tx")
                            )
                                .mapValues({ it ->
                                    formatter.print(it)
                                }, Named.`as`("serialize-json"))
                                .to(topicOut, Produced.with(Serdes.String(), Serdes.String()).withName("output"))
                        }
                    )
            }.build()
    }
}
