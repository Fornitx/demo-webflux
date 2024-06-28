package com.example.demowebflux.rest

import com.example.demowebflux.metrics.DemoMetrics
import com.example.demowebflux.properties.DemoProperties
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ReloadableResourceBundleMessageSource
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class DemoConfig {
    @Bean
    fun client(webClientBuilder: WebClient.Builder, properties: DemoProperties, metrics: DemoMetrics): DemoClient {
        return DemoClient(webClientBuilder, properties.client, metrics)
    }

    @Bean
    fun service(
        properties: DemoProperties,
        client: DemoClient,
        messageSource: MessageSource,
    ): DemoService {
        return DemoService(properties.service, client, messageSource)
    }

    @Bean
    fun messageSource(): MessageSource {
        val messageSource = ReloadableResourceBundleMessageSource()
        messageSource.setBasename("classpath:messages")
        messageSource.setDefaultEncoding("UTF-8")
        return messageSource
    }
}
