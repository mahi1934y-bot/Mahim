package com.mahim.aisignalpro

enum class SignalDecision { UP, DOWN, NO_TRADE }

data class SignalResult(
    val market: String?,
    val decision: SignalDecision,
    val confidence: Int,
    val timeframe: String = "1M",
    val expiry: String = "1M",
    val status: String
)
