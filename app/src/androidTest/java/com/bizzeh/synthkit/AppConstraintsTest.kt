package com.bizzeh.synthkit

import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppConstraintsTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun appRequestsNoNetworkPermission() {
        val info = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS,
        )
        val requested = info.requestedPermissions.orEmpty().toList()

        assertFalse(requested.contains(android.Manifest.permission.INTERNET))
    }

    @Test
    fun mainActivityIsLandscapeOnly() {
        val activity = context.packageManager.getActivityInfo(
            android.content.ComponentName(context, MainActivity::class.java),
            0,
        )

        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, activity.screenOrientation)
    }
}
