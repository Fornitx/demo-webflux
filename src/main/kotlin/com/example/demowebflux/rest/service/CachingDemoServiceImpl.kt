package com.example.demowebflux.rest.service

import com.example.demowebflux.data.DemoRequest
import com.example.demowebflux.data.DemoResponse
import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future

class CachingDemoServiceImpl(private val service: DemoServiceImpl) : DemoService {
    private val cache = Caffeine.newBuilder()
        .maximumSize(10)
        .buildAsync<DemoRequest, DemoResponse>()

    override suspend fun foo(request: DemoRequest): DemoResponse {
        return coroutineScope {
            cache.get(request) { key, _ -> future { service.foo(key) } }.await()
        }
    }
}
