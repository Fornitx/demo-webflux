package com.example.demowebflux.rest.service

import com.example.demowebflux.data.DemoRequest
import com.example.demowebflux.data.DemoResponse
import com.example.demowebflux.metrics.DemoMetrics
import com.github.benmanes.caffeine.cache.Caffeine
import java.time.Instant

class CachingDemoServiceImpl(private val service: DemoServiceImpl, metrics: DemoMetrics) : DemoService {
    private val cache = Caffeine.newBuilder()
        .maximumSize(10)
        .recordStats()
        .build<DemoRequest, DemoResponse>()

    init {
        metrics.cacheHits(cache)
        metrics.cacheMiss(cache)
    }

    override suspend fun foo(request: DemoRequest): DemoResponse {
        var response = cache.getIfPresent(request)?.copy(instant = Instant.now())
        if (response == null) {
            response = service.foo(request)
            cache.put(request, response)
        }
        return response
    }
}
