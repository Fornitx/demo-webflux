plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
}

group = "com.example"
version = "1.0.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

ext["kotlin-coroutines.version"] = System.getProperty("kotlin_coroutines_version")

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")

    implementation("io.github.oshai:kotlin-logging-jvm:" + System.getProperty("kotlin_logging_version"))

    implementation("net.logstash.logback:logstash-logback-encoder:7.4")

    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.6.0")

    implementation("org.bitbucket.b_c:jose4j:0.9.6")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug")

    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")

    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")

//    testImplementation("org.wiremock:wiremock-standalone:3.5.2")

    testImplementation("io.projectreactor.tools:blockhound:1.0.9.RELEASE")
    testImplementation("io.projectreactor.tools:blockhound-junit-platform:1.0.9.RELEASE")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xemit-jvm-type-annotations")
    }
}

tasks.withType<Test> {
    jvmArgs("-XX:+AllowRedefinitionToAddDeleteMethods")
    useJUnitPlatform()
}

tasks.jar {
    enabled = false
}
