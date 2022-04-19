package com.okp4.processor.cosmos

import com.google.protobuf.Any
import com.google.protobuf.GeneratedMessageV3
import com.google.protobuf.util.JsonFormat
import cosmos.bank.v1beta1.Tx
import cosmos.base.v1beta1.CoinOuterClass
import io.github.classgraph.ClassGraph
import protoTypeRegistry

val msgSendFromAdress = "okp414feusgeu79lf799efcmrnr3zkjly54kr9epx58"
val msgSendToAdress = "okp418m8pg88n9yyr6hsrwylcy34cz3fgar3gk6ctun"
val msgSendAmount = "32know"
val coin = CoinOuterClass.Coin.newBuilder().setDenom("know").setAmount(msgSendAmount)
val tx = cosmos.bank.v1beta1.Tx.MsgSend.newBuilder().addAmount(coin).setFromAddress(msgSendFromAdress)
    .setToAddress(msgSendToAdress).build()

val txBinary = Any.pack(tx).toByteArray()

fun main() {
    // given a type registry
    val registry: JsonFormat.TypeRegistry =
        protoTypeRegistry()

    // given a binary serialized transaction
    val msgSendFromAdress = "okp414feusgeu79lf799efcmrnr3zkjly54kr9epx58"
    val msgSendToAdress = "okp418m8pg88n9yyr6hsrwylcy34cz3fgar3gk6ctun"
    val msgSendAmount = "32know"
    val coin = CoinOuterClass.Coin.newBuilder().setDenom("know").setAmount(msgSendAmount)
    val tx = Any.pack(Tx.MsgSend.newBuilder().addAmount(coin).setFromAddress(msgSendFromAdress)
        .setToAddress(msgSendToAdress).build()).toByteArray()

    // then, we print in JSON like this:
    val unmarshalled = Any.parseFrom(tx)

    println(JsonFormat.printer().usingTypeRegistry(registry).print(unmarshalled))

    /**
     * Method method = clazz.getMethod("methodName", String.class);
    Object o = method.invoke(null, "whatever");
     *
     *  public static final Descriptor getDescriptor() {
    return Tx.internal_static_cosmos_bank_v1beta1_MsgSend_descriptor;
    }
     */
}
