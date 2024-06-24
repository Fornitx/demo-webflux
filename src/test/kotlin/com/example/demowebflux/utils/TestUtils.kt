package com.example.demowebflux.utils

import java.util.*
import kotlin.random.Random

fun requestId(): String = HexFormat.of().formatHex(Random.nextBytes(8))
