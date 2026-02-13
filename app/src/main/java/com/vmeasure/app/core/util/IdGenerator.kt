package com.vmeasure.app.core.util

import kotlin.random.Random

object IdGenerator {
    fun random6Digits(): String {
        // 100000..999999 inclusive
        val n = Random.nextInt(100000, 1_000_000)
        return n.toString()
    }
}
