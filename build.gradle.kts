import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.FileOutputStream
import java.util.zip.ZipFile

plugins {
    kotlin("jvm") version "1.8.10"
    kotlin("plugin.allopen") version "1.6.10"
    id("io.quarkus")

    id("maven-publish")

    id("org.jlleitschuh.gradle.ktlint") version "10.2.1"
    id("io.gitlab.arturbosch.detekt") version "1.20.0"
}

ktlint {
    version.set("0.45.2")
}

group = "com.okp4"
description = """A Kafka Streams Processor using Quarkus that consumes CØSMOS protobuf messages and send a
json decoded message in the output topic"""

val pullReflectionConfig: Configuration by configurations.creating
configurations {
    implementation.extendsFrom(pullReflectionConfig)
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

fun prepareVersion(): String {
    val digits = (project.property("project.version") as String).split(".")
    if (digits.size != 3) {
        throw GradleException("Wrong 'project.version' specified in properties, expects format 'x.y.z'")
    }

    return digits.map { it.toInt() }
        .let {
            it.takeIf { it[2] == 0 }?.subList(0, 2) ?: it
        }.let {
            it.takeIf { !project.hasProperty("release") }?.mapIndexed { i, d ->
                if (i == 1) d + 1 else d
            } ?: it
        }.joinToString(".") + project.hasProperty("release").let { if (it) "" else "-SNAPSHOT" }
}

afterEvaluate {
    project.version = prepareVersion()
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/okp4/okp4-cosmos-proto")
        credentials {
            username = project.property("maven.credentials.username") as String
            password = project.property("maven.credentials.password") as String
        }
    }
}

dependencies {
    implementation(enforcedPlatform("$quarkusPlatformGroupId:$quarkusPlatformArtifactId:$quarkusPlatformVersion"))
    implementation(enforcedPlatform("$quarkusPlatformGroupId:quarkus-camel-bom:$quarkusPlatformVersion"))
    implementation("io.quarkus:quarkus-core-deployment")
    implementation("io.quarkus:quarkus-grpc")
    implementation("io.quarkus:quarkus-kotlin")
    implementation("org.apache.camel.quarkus:camel-quarkus-protobuf")
    implementation("io.quarkus:quarkus-kafka-streams")
    implementation("io.quarkus:quarkus-micrometer")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-smallrye-health")

    val cosmosSdkVersion = "1.2"
    implementation("com.okp4.grpc:cosmos-sdk:$cosmosSdkVersion")
    pullReflectionConfig("com.okp4.grpc:cosmos-sdk:$cosmosSdkVersion")

    val grpcVersion = "1.46.0"
    implementation("io.grpc:grpc-protobuf:$grpcVersion")
    implementation("io.github.classgraph:classgraph:4.8.147")

    testImplementation(kotlin("test"))

    val kotestVersion = "5.2.3"
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-property:$kotestVersion")
    testImplementation("io.kotest:kotest-framework-datatest:$kotestVersion")

    val kafkaStreamVersion = "3.1.0"
    testImplementation("org.apache.kafka:kafka-streams-test-utils:$kafkaStreamVersion")

    implementation("io.kotest:kotest-assertions-json:5.2.3")
}

allOpen {
    annotation("javax.ws.rs.Path")
    annotation("javax.enterprise.context.ApplicationScoped")
    annotation("io.quarkus.test.junit.QuarkusTest")
}

tasks {
    val task = register("reflectionConfig") {
        val configPath = "reflection-config.json"
        val configs = pullReflectionConfig.files.joinToString(",") {
            val zip = ZipFile(it)
            val entry = zip.getEntry(configPath)

            buildDir.mkdirs()
            val out = File(buildDir, "${it.name}-${entry.name}")
            out.createNewFile()
            zip.getInputStream(entry).transferTo(FileOutputStream(out))

            out.path
        }

        System.setProperty("quarkus.native.additional-build-args", "-H:ReflectionConfigurationFiles=$configs")
    }

    build {
        dependsOn.add(task)
    }
}

tasks.register("lint") {
    dependsOn.addAll(listOf("ktlintCheck", "detekt"))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    testLogging {
        events("PASSED", "SKIPPED", "FAILED")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.apply {
        jvmTarget = "11"
        allWarningsAsErrors = true
    }
}

tasks.named<KotlinCompile>("compileTestKotlin") {
    kotlinOptions.apply {
        jvmTarget = "11"
        allWarningsAsErrors = false
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/okp4/${project.name}")
            credentials {
                username = project.property("maven.credentials.username") as String
                password = project.property("maven.credentials.password") as String
            }
        }
    }
}
