plugins {
    id("org.springframework.boot") version System.getProperty("spring.version")
    id("io.spring.dependency-management") version System.getProperty("spring.dm.version")
    kotlin("jvm") version System.getProperty("kotlin.version")
    kotlin("plugin.spring") version System.getProperty("kotlin.version")
}

group = "com.example"
version = "1.0.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_19
    targetCompatibility = JavaVersion.VERSION_19
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")

    implementation("org.jetbrains.kotlin:kotlin-reflect")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8")

    implementation("io.github.oshai:kotlin-logging-jvm:4.0.0-beta-22")

    implementation("net.logstash.logback:logstash-logback-encoder:7.3")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")

    testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
}

tasks.compileKotlin {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "19"
    }
}

tasks.test {
    useJUnitPlatform()
}
