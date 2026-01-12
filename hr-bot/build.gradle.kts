plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
    id("com.github.johnrengelman.shadow")
}

group = "com.hrbot"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // Telegram Bot
    implementation("io.github.kotlin-telegram-bot.kotlin-telegram-bot:telegram:6.1.0")
    
    // HTTP Client (Ktor) - needed for ZenRows
    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("io.ktor:ktor-client-cio:2.3.7")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
    
    // HTML Parsing (if needed after ZenRows, though ZenRows can return clean HTML/JSON)
    implementation("org.jsoup:jsoup:1.17.2")
    
    // Dependency Injection
    implementation("io.insert-koin:koin-core:3.5.3")
    
    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    
    // OpenAI Client
    implementation("com.aallam.openai:openai-client:3.7.0")
    
    // Testing
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
}

application {
    mainClass.set("com.hrbot.MainKt")
}

tasks.named<JavaExec>("run") {
    // Pass all environment variables from the host to the application
    environment(System.getenv())
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    archiveBaseName.set("hr-bot")
    archiveClassifier.set("all")
    mergeServiceFiles()
}
