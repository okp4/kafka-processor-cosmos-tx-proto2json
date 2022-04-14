package com.okp4.processor.cosmos

import com.google.protobuf.Any
import cosmos.base.v1beta1.CoinOuterClass
import io.kotest.assertions.json.shouldEqualSpecifiedJson
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import okp4.okp4d.knowledge.DataspaceOuterClass
import okp4.okp4d.knowledge.Tx
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TopologyTestDriver

class TopologyTest : BehaviorSpec({
    val stringSerde = Serdes.StringSerde()
    val byteArraySerde = Serdes.ByteArraySerde()
    val config = mapOf(
        StreamsConfig.APPLICATION_ID_CONFIG to "simple",
        StreamsConfig.BOOTSTRAP_SERVERS_CONFIG to "dummy:1234",
        "topic.in" to "in",
        "topic.out" to "out"
    ).toProperties()

    given("A topology") {
        val topology = topology(config)
        val testDriver = TopologyTestDriver(topology, config)
        val inputTopic = testDriver.createInputTopic("in", stringSerde.serializer(), byteArraySerde.serializer())
        val outputTopic = testDriver.createOutputTopic("out", stringSerde.deserializer(), stringSerde.deserializer())

        val dataspaceId = "datespaceId1"
        val dataspaceName = "datespaceName1"
        val dataSpaceMessage = Any.pack<DataspaceOuterClass.Dataspace>(
            DataspaceOuterClass.Dataspace.newBuilder().setName(dataspaceName).setId(dataspaceId).build()
        ).toByteArray()

        val bangDataspaceName = "Invasion plan B"
        val bangDataspaceCreator = "DarkLordBunny"
        val bangDataspaceDescription = "Get some carrots and finish the invasion"
        val msgBangDataspaceBlock = Any.pack<Tx.MsgBangDataspace>(
            Tx.MsgBangDataspace.newBuilder().setName(bangDataspaceName).setCreator(bangDataspaceCreator)
                .setDescription(bangDataspaceDescription).build()
        ).toByteArray()

        val msgSendFromAdress = "okp414feusgeu79lf799efcmrnr3zkjly54kr9epx58"
        val msgSendToAdress = "okp418m8pg88n9yyr6hsrwylcy34cz3fgar3gk6ctun"
        val msgSendAmount = "32know"
        val coin = CoinOuterClass.Coin.newBuilder().setDenom("know").setAmount(msgSendAmount)
        val msgSend = Any.pack<cosmos.bank.v1beta1.Tx.MsgSend>(
            cosmos.bank.v1beta1.Tx.MsgSend.newBuilder().addAmount(coin).setFromAddress(msgSendFromAdress)
                .setToAddress(msgSendToAdress).build()
        ).toByteArray()

        `when`("sending a Dataspace creation message to the input topic ($inputTopic)") {
            inputTopic.pipeInput("", dataSpaceMessage as ByteArray)

            then("a json version of the message is received in the output topic ($outputTopic)") {
                val result = outputTopic.readValue()
                result shouldNotBe null

                result shouldEqualSpecifiedJson """ {"id":"$dataspaceId","name":"$dataspaceName"} """
            }
        }
        `when`("An invalid message ends up in the input topic") {
            inputTopic.pipeInput("", "I'm undecodable !!!".toByteArray())

            then("The error is managed, nothing is send to the output topic, the stream processor is still running") {
                val errorResult = outputTopic.readValue()
                errorResult shouldBe null
            }
        }
        `when`("two other transactions") {
            inputTopic.pipeInput("", msgBangDataspaceBlock)
            inputTopic.pipeInput("", msgSend)

            then("Two messages corresponding to the sent transaction should be received") {
                val resultDataspaceCreation = outputTopic.readValue()
                resultDataspaceCreation shouldEqualSpecifiedJson """
                {"name":"$bangDataspaceName",
                "creator":"$bangDataspaceCreator",
                "description":"$bangDataspaceDescription"}
                """.trimIndent()

                val resultMsgTransaction = outputTopic.readValue()
                resultMsgTransaction shouldEqualSpecifiedJson """
                    {"fromAddress":"$msgSendFromAdress",
                    "toAddress":"$msgSendToAdress",
                    "amount":[{"denom":"know","amount":"$msgSendAmount"}]
                    }
                    """.trimMargin()
            }
        }
    }
})
