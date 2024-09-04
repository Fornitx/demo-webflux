package com.example.demowebflux.utils

import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext

suspend inline fun <T> withMDCContextAsync(
    pair: Pair<String, String>,
    crossinline body: suspend () -> T,
): T = withContext(MDCContext(mapOf(pair))) { body() }

suspend inline fun <T> withMDCContextAsync(
    vararg pair: Pair<String, String>,
    crossinline body: suspend () -> T,
): T = withContext(MDCContext(mapOf(*pair))) { body() }

suspend inline fun <T> withMDCContextAsync(
    map: Map<String, String>,
    crossinline body: suspend () -> T,
): T = withContext(MDCContext(map)) { body() }
