package com.example.demowebflux.rest.proxy.errors

import com.example.demowebflux.constants.API_V2
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod

@SpringBootTest
class ProxyGet5xxErrorsTest : Proxy5xxErrorsTest(
    HttpMethod.GET,
    "$API_V2/testPath?testParam=123",
    null,
    "/testPath",
)
