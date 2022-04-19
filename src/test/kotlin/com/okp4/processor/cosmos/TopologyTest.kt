package com.okp4.processor.cosmos

import com.google.protobuf.Any
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import okp4.okp4d.knowledge.Tx
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TopologyTestDriver
import protoTypeRegistry

data class TestData(val tx: ByteArray, val expected: String)

class TopologyTest : BehaviorSpec({
    val stringSerde = Serdes.StringSerde()
    val byteArraySerde = Serdes.ByteArraySerde()
    val config = mapOf(
        StreamsConfig.APPLICATION_ID_CONFIG to "simple",
        StreamsConfig.BOOTSTRAP_SERVERS_CONFIG to "dummy:1234",
        "topic.in" to "in",
        "topic.out" to "out"
    ).toProperties()
    val typeRegistry = protoTypeRegistry()

    given("A topology") {
        val topology = topology(config, typeRegistry)
        val testDriver = TopologyTestDriver(topology, config)
        val inputTopic = testDriver.createInputTopic("in", stringSerde.serializer(), byteArraySerde.serializer())
        val outputTopic = testDriver.createOutputTopic("out", stringSerde.deserializer(), stringSerde.deserializer())

        withData(
            mapOf(
                "transaction 1" to TestData(
                    Any.pack(
                        Tx.MsgBangDataspace.newBuilder().setName("Invasion plan B").setCreator("DarkLordBunny")
                            .setDescription("Get some carrots and finish the invasion").build()
                    ).toByteArray(), """{
  "@type": "type.googleapis.com/okp4.okp4d.knowledge.MsgBangDataspace",
  "creator": "DarkLordBunny",
  "name": "Invasion plan B",
  "description": "Get some carrots and finish the invasion"
}"""
                )
            )
        ) { (tx, expected) ->
            and("a serialized transaction") {

                `when`("sending the transaction to the input topic ($inputTopic)") {
                    inputTopic.pipeInput("", tx)

                    then("a json version of the message is received in the output topic ($outputTopic)") {
                        val result = outputTopic.readValue()
                        result shouldNotBe null
                        result shouldBe expected
                    }
                }
            }
        }
    }
})
