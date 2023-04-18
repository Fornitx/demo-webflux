package com.example.demowebflux.rest

import com.example.demowebflux.data.DemoRequest
import com.example.demowebflux.data.DemoResponse
import com.example.demowebflux.errors.DemoError
import com.example.demowebflux.errors.DemoRestException
import io.github.oshai.KotlinLogging
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class DemoService {
    suspend fun foo(request: DemoRequest): DemoResponse {
        log.info { "Service calling" }
        if (request.msg == "666") {
            throw DemoRestException(DemoError.MSG_IS_666)
        }
        return DemoResponse(request.msg.repeat(3), others = request.others)
    }
}
