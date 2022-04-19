package com.okp4.processor.cosmos.json

import com.google.protobuf.Descriptors
import com.google.protobuf.GeneratedMessageV3
import com.google.protobuf.util.JsonFormat
import io.github.classgraph.ClassGraph
import org.slf4j.LoggerFactory

val protoTypeRegistry: () -> JsonFormat.TypeRegistry = {
    val logger = LoggerFactory.getLogger("com.okp4.processor.cosmos.json.type-registry")

    ClassGraph().enableAllInfo().scan().use { scanResult ->
        scanResult
            .getSubclasses(GeneratedMessageV3::class.java)
            .mapNotNull { classInfo ->
                runCatching {
                    val method = classInfo.loadClass().getMethod("getDescriptor")
                    method.invoke(null) as Descriptors.Descriptor
                }.onSuccess {
                    logger.debug("Registering type <${it.name}>")
                }.onFailure { e ->
                    logger.warn("Failed to get descriptor for class <${classInfo.name}>", e)
                }.getOrNull()
            }
            .fold(JsonFormat.TypeRegistry.newBuilder()) { registry, descriptor -> registry.add(descriptor) }.build()
    }
}
