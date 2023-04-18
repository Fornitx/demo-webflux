package com.example.demowebflux

import com.example.demowebflux.properties.DemoProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(DemoProperties::class)
class DemoWebfluxApplication

fun main(args: Array<String>) {
	runApplication<DemoWebfluxApplication>(*args)
}
