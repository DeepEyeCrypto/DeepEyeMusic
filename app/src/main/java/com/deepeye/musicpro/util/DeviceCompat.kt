package com.deepeye.musicpro.util

import android.os.Build
import android.app.ActivityManager
import android.content.Context

object DeviceCompat {
    val isVivo: Boolean
        get() = Build.MANUFACTURER.equals("Vivo", ignoreCase = true)

    val isAndroid16: Boolean
        get() = Build.VERSION.SDK_INT >= 35 // VANILLA_ICE_CREAM

    fun isLowRamDevice(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return activityManager.isLowRamDevice
    }

    fun getSurfaceType(context: Context): String {
        if (isVivo || isLowRamDevice(context)) {
            return "surface_view"  // Vivo prefers SurfaceView
        }
        return "texture_view"  // Others use TextureView
    }

    fun getRendererType(): String {
        if (isAndroid16 && isVivo) {
            return "OpenGL_ES"  // Force OpenGL on Vivo + Android 16
        }
        return "Vulkan"  // Default Vulkan
    }

    fun shouldForceSoftwareRenderer(context: Context): Boolean {
        return isVivo && isLowRamDevice(context)
    }
}
