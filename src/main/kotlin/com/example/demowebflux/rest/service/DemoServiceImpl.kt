package com.example.demowebflux.rest.service

import com.example.demowebflux.data.DemoRequest
import com.example.demowebflux.data.DemoResponse
import com.example.demowebflux.errors.DemoError
import com.example.demowebflux.errors.DemoRestException
import com.example.demowebflux.properties.DemoProperties.ServiceProperties
import com.example.demowebflux.rest.client.DemoClient
import io.github.oshai.KotlinLogging

private val log = KotlinLogging.logger {}

class DemoServiceImpl(
    private val properties: ServiceProperties,
    private val client: DemoClient,
) : DemoService {
    override suspend fun foo(request: DemoRequest): DemoResponse {
        log.info { "Service calling" }
        if (request.msg == "666") {
            throw DemoRestException(DemoError.MSG_IS_666)
        }
        return DemoResponse(client.call(request.msg).repeat(properties.multiplier), others = request.others)
    }
}
