plugins {
	id("org.springframework.boot") version System.getProperty("springVersion")
	id("io.spring.dependency-management") version System.getProperty("springDMVersion")
	kotlin("jvm") version System.getProperty("kotlinVersion")
	kotlin("plugin.spring") version System.getProperty("kotlinVersion")
}

group = "com.example"
java.sourceCompatibility = JavaVersion.VERSION_18
java.targetCompatibility = JavaVersion.VERSION_18

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webflux")

	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
	implementation("io.github.microutils:kotlin-logging-jvm:2.1.23")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("io.projectreactor:reactor-test")
	testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
	testImplementation("org.apache.commons:commons-lang3")

	testImplementation("com.squareup.okhttp3:mockwebserver:4.10.0")
	testImplementation("com.squareup.okhttp3:okhttp:4.10.0")
}

tasks.compileKotlin {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "18"
	}
}

tasks.test {
	useJUnitPlatform()
}
