package com.bizzeh.synthkit.audio

import android.annotation.SuppressLint
import android.media.AudioDeviceInfo

/** Why tap-to-sound delay may be high. Play is never blocked; the player is only told. */
enum class LatencyWarning { BLUETOOTH, NOT_LOW_LATENCY }

enum class OutputKind { BLUETOOTH, OTHER, UNKNOWN }

object LatencyWarnings {
    private const val LOW_LATENCY_MODE = "LowLatency"

    // TYPE_BLE_HEADSET, TYPE_BLE_SPEAKER (API 31) and TYPE_BLE_BROADCAST (API 33)
    // are compile-time constants; on older devices they simply never match.
    @SuppressLint("InlinedApi")
    private val BLUETOOTH_TYPES = setOf(
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        AudioDeviceInfo.TYPE_BLE_HEADSET,
        AudioDeviceInfo.TYPE_BLE_SPEAKER,
        AudioDeviceInfo.TYPE_BLE_BROADCAST,
    )

    fun isBluetooth(deviceType: Int): Boolean = deviceType in BLUETOOTH_TYPES

    /** Bluetooth takes priority: it adds far more delay than a missing low-latency mode. */
    fun evaluate(report: LatencyReport?, output: OutputKind): LatencyWarning? = when {
        report == null -> null
        output == OutputKind.BLUETOOTH -> LatencyWarning.BLUETOOTH
        report.performanceMode != LOW_LATENCY_MODE -> LatencyWarning.NOT_LOW_LATENCY
        else -> null
    }
}

/** A warning the player dismissed stays hidden until the reason changes. */
class WarningDismissal {
    private var dismissed: LatencyWarning? = null

    fun visible(current: LatencyWarning?): LatencyWarning? {
        if (current == null) dismissed = null
        return current?.takeIf { it != dismissed }
    }

    fun dismiss(warning: LatencyWarning) {
        dismissed = warning
    }
}
