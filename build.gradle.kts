import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version System.getProperty("spring_version")
    id("io.spring.dependency-management") version System.getProperty("spring_dm_version")
    kotlin("jvm") version System.getProperty("kotlin_version")
    kotlin("plugin.spring") version System.getProperty("kotlin_version")
}

group = "com.example"
version = "1.0.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_20
    targetCompatibility = JavaVersion.VERSION_20
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
//    implementation("org.springframework.boot:spring-boot-starter-cache")
//    implementation("org.springframework.boot:spring-boot-starter-security")
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

    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.2.0")

    implementation("com.google.guava:guava:32.1.2-jre")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    implementation("org.bitbucket.b_c:jose4j:0.9.3")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
//    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("io.projectreactor:reactor-test")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")

    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")

    testImplementation("com.squareup.okhttp3:mockwebserver3-junit5:5.0.0-alpha.11")
    testImplementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.11")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict", "-Xemit-jvm-type-annotations")
        jvmTarget = "20"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
