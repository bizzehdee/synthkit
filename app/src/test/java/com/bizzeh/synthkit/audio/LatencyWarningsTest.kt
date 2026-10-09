package com.bizzeh.synthkit.audio

import android.media.AudioDeviceInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LatencyWarningsTest {
    private val lowLatency = LatencyReport(5.0, "AAudio", "LowLatency", "Exclusive", 48000, 96, 192, 0)

    @Test
    fun lowLatencyStreamOnTheSpeakerNeedsNoWarning() {
        assertNull(LatencyWarnings.evaluate(lowLatency, OutputKind.OTHER))
        assertNull(LatencyWarnings.evaluate(lowLatency, OutputKind.UNKNOWN))
    }

    @Test
    fun bluetoothOutputWarns() {
        assertEquals(LatencyWarning.BLUETOOTH, LatencyWarnings.evaluate(lowLatency, OutputKind.BLUETOOTH))
    }

    @Test
    fun streamWithoutLowLatencyModeWarns() {
        val shared = lowLatency.copy(performanceMode = "None")

        assertEquals(LatencyWarning.NOT_LOW_LATENCY, LatencyWarnings.evaluate(shared, OutputKind.OTHER))
    }

    @Test
    fun bluetoothTakesPriorityOverMissingLowLatency() {
        val shared = lowLatency.copy(performanceMode = "PowerSaving")

        assertEquals(LatencyWarning.BLUETOOTH, LatencyWarnings.evaluate(shared, OutputKind.BLUETOOTH))
    }

    @Test
    fun noRunningStreamNeedsNoWarning() {
        assertNull(LatencyWarnings.evaluate(null, OutputKind.BLUETOOTH))
    }

    @Test
    fun bluetoothDeviceTypesAreRecognised() {
        assertTrue(LatencyWarnings.isBluetooth(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP))
        assertTrue(LatencyWarnings.isBluetooth(AudioDeviceInfo.TYPE_BLE_HEADSET))
        assertFalse(LatencyWarnings.isBluetooth(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER))
        assertFalse(LatencyWarnings.isBluetooth(AudioDeviceInfo.TYPE_WIRED_HEADPHONES))
    }

    @Test
    fun dismissedWarningStaysHiddenUntilTheReasonChanges() {
        val dismissal = WarningDismissal()
        dismissal.dismiss(LatencyWarning.BLUETOOTH)

        assertNull(dismissal.visible(LatencyWarning.BLUETOOTH))
        assertEquals(LatencyWarning.NOT_LOW_LATENCY, dismissal.visible(LatencyWarning.NOT_LOW_LATENCY))
    }

    @Test
    fun warningReturnsAfterItsCauseClearsAndComesBack() {
        val dismissal = WarningDismissal()
        dismissal.dismiss(LatencyWarning.BLUETOOTH)

        assertNull(dismissal.visible(null))
        assertEquals(LatencyWarning.BLUETOOTH, dismissal.visible(LatencyWarning.BLUETOOTH))
    }
}
