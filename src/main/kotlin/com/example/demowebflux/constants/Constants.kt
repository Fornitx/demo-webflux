package com.example.demowebflux.constants

const val PREFIX = "demo"

const val API = "/api"
const val V1 = "/v1"
const val V2 = "/v2"
const val API_V1 = API + V1
const val API_V2 = API + V2

// HTTP headers
const val HEADER_X_REQUEST_ID = "X-Request-Id"

// exchange attribute keys / reactor context keys
const val ATTRIBUTE_REQUEST_ID = "requestId"
const val ATTRIBUTE_DEMO_TOKEN = "demoToken"

// logstash args
const val LOGSTASH_REQUEST_ID = "args.requestId"
