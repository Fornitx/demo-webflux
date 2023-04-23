package com.example.demowebflux.rest

import com.example.demowebflux.metrics.DemoMetrics
import com.example.demowebflux.properties.DemoProperties
import com.example.demowebflux.rest.client.DemoClient
import com.example.demowebflux.rest.service.CachingDemoServiceImpl
import com.example.demowebflux.rest.service.DemoService
import com.example.demowebflux.rest.service.DemoServiceImpl
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DemoConfig {
    @Bean
    fun service(properties: DemoProperties, client: DemoClient, metrics: DemoMetrics): DemoService {
        val service = DemoServiceImpl(properties.service, client)
        return if (properties.service.cache) {
            CachingDemoServiceImpl(service, metrics)
        } else {
            service
        }
    }
}
