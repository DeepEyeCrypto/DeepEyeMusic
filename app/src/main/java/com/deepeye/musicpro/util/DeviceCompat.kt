package com.deepeye.musicpro.util

import android.os.Build
import android.app.ActivityManager
import android.content.Context
import android.util.Log
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "DeviceCompat"

/**
 * Total RAM (MB) below which a device is treated as low-tier for glassmorphism.
 * Chosen so blur is disabled on the hardware class where offscreen render passes
 * measurably starve the audio render thread, and enabled above it.
 */
private const val LOW_TIER_RAM_MB = 3072L

object DeviceCompat {
    val isVivo: Boolean
        get() = Build.MANUFACTURER.equals("Vivo", ignoreCase = true)

    val isAndroid16: Boolean
        get() = Build.VERSION.SDK_INT >= 35 // VANILLA_ICE_CREAM

    fun isLowRamDevice(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return activityManager.isLowRamDevice
    }

    /**
     * Whether this device should skip real-time glassmorphism blur.
     *
     * Blur (Haze / RenderEffect) is fill-rate bound: every blurred surface costs a
     * full-screen offscreen pass per frame. On low-tier hardware — cheap Android TV
     * boxes, low-RAM phones, and software-rendered devices — that is the single
     * biggest source of dropped frames in this app, and it competes directly with
     * the audio/DSP render thread for memory bandwidth.
     *
     * Signals used, cheapest first:
     * - [ActivityManager.isLowRamDevice]: the platform's own low-memory judgement.
     * - total RAM below the practical floor for offscreen blur buffers.
     * - hardware acceleration explicitly disabled.
     *
     * Result is memoized per-Context because it is read from every glass surface
     * during composition, and the underlying values cannot change at runtime.
     */
    private val lowTierCache = ConcurrentHashMap<Context, Boolean>()

    fun isLowTierDevice(context: Context): Boolean = lowTierCache.getOrPut(context.applicationContext) {
        try {
            val activityManager =
                context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val lowRam = activityManager?.isLowRamDevice ?: false

            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager?.getMemoryInfo(memoryInfo)
            val totalRamMb = memoryInfo.totalMem / (1024L * 1024L)

            // 3 GB is the floor: below this, blur buffers plus the ExoPlayer +
            // AudioTrack + DSP graph allocations thrash the heap.
            lowRam || totalRamMb in 1 until LOW_TIER_RAM_MB
        } catch (e: Exception) {
            Log.w(TAG, "Low-tier detection failed, assuming capable device: ${e.message}")
            false
        }
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
