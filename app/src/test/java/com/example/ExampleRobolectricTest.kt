package com.example

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import com.example.service.NotificationCaptureService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("NotifVault", appName)
    }

    @Test
    fun `manifest declares NotificationCaptureService and has no internet permission`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pm = context.packageManager

        val pkgInfo = pm.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS or PackageManager.GET_SERVICES
        )
        val permissions = pkgInfo.requestedPermissions?.toList().orEmpty()
        assertFalse("App must not declare INTERNET permission", permissions.contains(Manifest.permission.INTERNET))

        val serviceInfo = pm.getServiceInfo(
            ComponentName(context, NotificationCaptureService::class.java),
            0
        )
        assertNotNull(serviceInfo)
        assertEquals(
            Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE,
            serviceInfo.permission
        )
    }
}
