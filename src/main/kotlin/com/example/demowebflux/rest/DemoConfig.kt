package com.example.demowebflux.rest

import com.example.demowebflux.metrics.DemoMetrics
import com.example.demowebflux.properties.DemoProperties
import com.example.demowebflux.rest.client.DemoClient
import com.example.demowebflux.rest.client.DemoClientImpl
import com.example.demowebflux.rest.service.CachingDemoServiceImpl
import com.example.demowebflux.rest.service.DemoService
import com.example.demowebflux.rest.service.DemoServiceImpl
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ReloadableResourceBundleMessageSource

@Configuration
class DemoConfig {
    @Bean
    fun client(properties: DemoProperties): DemoClient {
        return DemoClientImpl(properties.client)
    }

    @Bean
    fun service(
        properties: DemoProperties,
        client: DemoClient,
        messageSource: MessageSource,
        metrics: DemoMetrics
    ): DemoService {
        val service = DemoServiceImpl(properties.service, client, messageSource)
        return if (properties.service.cache) {
            CachingDemoServiceImpl(service, metrics)
        } else {
            service
        }
    }

    @Bean
    fun messageSource(): MessageSource {
        val messageSource = ReloadableResourceBundleMessageSource()
        messageSource.setBasename("classpath:messages")
        messageSource.setDefaultEncoding("UTF-8")
        return messageSource
    }
}
