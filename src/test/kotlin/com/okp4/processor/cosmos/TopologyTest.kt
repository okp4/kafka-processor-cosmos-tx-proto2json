package com.okp4.processor.cosmos

import com.google.protobuf.ByteString.copyFrom
import com.okp4.processor.cosmos.json.TopologyProducer
import cosmos.tx.v1beta1.TxOuterClass
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.data.forAll
import io.kotest.data.headers
import io.kotest.data.row
import io.kotest.data.table
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TopologyTestDriver
import java.util.Base64.getDecoder

fun String.b64ToByteString() = copyFrom(getDecoder().decode(this))

val tx1 = TxOuterClass.TxRaw.newBuilder()
    .addSignatures("2utl1VHdSC3pyHCNgeNmgGImnEChQcd9sWEgi4Uc4lwOhWhrqYy8WkJ8xNkVzjF/WVg3ayVWZp8ipVzO1kUK9g==".b64ToByteString())
    .setAuthInfoBytes("Ck4KRgofL2Nvc21vcy5jcnlwdG8uc2VjcDI1NmsxLlB1YktleRIjCiECf1JPoIG8+pMDKtmH2vtOg5+xvfNxoDXV0iD++Ha5a/0SBAoCCAESBBDAmgw=".b64ToByteString())
    .setBodyBytes(
        "CoUBChwvY29zbW9zLmJhbmsudjFiZXRhMS5Nc2dTZW5kEmUKK29rcDQxcmhkODc0NHU0dnF2Y2p1dnlmbThmZWE0azltZWZlM2s1N3F6MjcSK29rcDQxOTY4NzdkajRjcnB4bWphMnd3MmhqMnZneTQ1djZ1c3Bremt0OGwaCQoEa25vdxIBMw==".b64ToByteString()
    ).build().toByteArray()
val txError = "nop".toByteArray()

val tx1Json =
    """{"body":{"messages":[{"@type":"/cosmos.bank.v1beta1.MsgSend","fromAddress":"okp41rhd8744u4vqvcjuvyfm8fea4k9mefe3k57qz27","toAddress":"okp4196877dj4crpxmja2ww2hj2vgy45v6uspkzkt8l","amount":[{"denom":"know","amount":"3"}]}]},"authInfo":{"signerInfos":[{"publicKey":{"@type":"/cosmos.crypto.secp256k1.PubKey","key":"An9ST6CBvPqTAyrZh9r7ToOfsb3zcaA11dIg/vh2uWv9"},"modeInfo":{"single":{"mode":"SIGN_MODE_DIRECT"}}}],"fee":{"gasLimit":"200000"}},"signatures":["2utl1VHdSC3pyHCNgeNmgGImnEChQcd9sWEgi4Uc4lwOhWhrqYy8WkJ8xNkVzjF/WVg3ayVWZp8ipVzO1kUK9g=="]}"""
val tx1JsonPrettyPrinted = """{
  "body": {
    "messages": [{
      "@type": "/cosmos.bank.v1beta1.MsgSend",
      "fromAddress": "okp41rhd8744u4vqvcjuvyfm8fea4k9mefe3k57qz27",
      "toAddress": "okp4196877dj4crpxmja2ww2hj2vgy45v6uspkzkt8l",
      "amount": [{
        "denom": "know",
        "amount": "3"
      }]
    }]
  },
  "authInfo": {
    "signerInfos": [{
      "publicKey": {
        "@type": "/cosmos.crypto.secp256k1.PubKey",
        "key": "An9ST6CBvPqTAyrZh9r7ToOfsb3zcaA11dIg/vh2uWv9"
      },
      "modeInfo": {
        "single": {
          "mode": "SIGN_MODE_DIRECT"
        }
      }
    }],
    "fee": {
      "gasLimit": "200000"
    }
  },
  "signatures": ["2utl1VHdSC3pyHCNgeNmgGImnEChQcd9sWEgi4Uc4lwOhWhrqYy8WkJ8xNkVzjF/WVg3ayVWZp8ipVzO1kUK9g=="]
}"""

class TopologyTest : BehaviorSpec({
    val stringSerde = Serdes.StringSerde()
    val byteArraySerde = Serdes.ByteArraySerde()

    table(
        headers("case", "tx", "pretty-print", "expected", "error"),
        row(
            1,
            tx1,
            null,
            tx1Json,
            false
        ),
        row(
            2,
            tx1,
            "false",
            tx1Json,
            false
        ),
        row(
            3,
            tx1,
            "foo",
            tx1Json,
            false
        ),
        row(
            4,
            tx1,
            "true",
            tx1JsonPrettyPrinted,
            false
        ),
        row(
            5,
            txError,
            "",
            null,
            true
        )
    ).forAll { case, tx, isPrettyPrint, expected, error ->
        given("A topology (for case <$case>)") {
            val config = mutableMapOf(
                StreamsConfig.APPLICATION_ID_CONFIG to "simple",
                StreamsConfig.BOOTSTRAP_SERVERS_CONFIG to "dummy:1234",
                "topic.in" to "in",
                "topic.out" to "out",
                "topic.error" to "error",
            )
                .apply {
                    if (isPrettyPrint != null) put("formatter.prettyPrint", isPrettyPrint)
                }
                .toProperties()

            val topology = TopologyProducer().apply {
                topicError = config.getProperty("topic.error")
                topicIn = config.getProperty("topic.in")
                topicOut = config.getProperty("topic.out")
                prettyPrint = config.getProperty("formatter.prettyPrint").toBoolean()
            }
                .buildTopology()
            val testDriver = TopologyTestDriver(topology, config)
            val inputTopic = testDriver.createInputTopic("in", stringSerde.serializer(), byteArraySerde.serializer())
            val outputTopic =
                testDriver.createOutputTopic("out", stringSerde.deserializer(), stringSerde.deserializer())
            val errorTopic =
                testDriver.createOutputTopic("error", stringSerde.deserializer(), byteArraySerde.deserializer())

            `when`("sending the transaction to the input topic ($inputTopic)") {
                inputTopic.pipeInput("", tx)

                if (!error) {
                    then("a json version of the message is received in the output topic ($outputTopic)") {
                        val result = outputTopic.readValue()
                        result shouldNotBe null
                        result shouldBe expected
                    }
                } else {
                    then("the faulty tx is received in the error topic ($errorTopic)") {
                        val result = errorTopic.readValue()
                        result shouldNotBe null
                        result shouldBe tx
                    }
                }
            }
        }
    }
})
