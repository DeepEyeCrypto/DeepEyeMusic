// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun rememberGyroscopeTilt(): State<Offset> {
    val context = LocalContext.current
    val tiltState = remember { mutableStateOf(Offset.Zero) }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(context, lifecycleOwner) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val rotationVectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val fallbackSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val activeSensor = rotationVectorSensor ?: fallbackSensor

        var currentX = 0f
        var currentY = 0f
        val kFilterFactor = 0.12f // 12% low-pass filter smoothing factor (exponential smoothing)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null || event.sensor == null) return

                var targetX = 0f
                var targetY = 0f

                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    targetX = (orientation[2] / (Math.PI / 2).toFloat()).coerceIn(-1.0f, 1.0f)
                    targetY = (orientation[1] / (Math.PI / 2).toFloat()).coerceIn(-1.0f, 1.0f)
                } else {
                    targetX = (event.values[0] / 9.81f).coerceIn(-1.0f, 1.0f)
                    targetY = (event.values[1] / 9.81f).coerceIn(-1.0f, 1.0f)
                }

                currentX += (targetX - currentX) * kFilterFactor
                currentY += (targetY - currentY) * kFilterFactor

                tiltState.value = Offset(currentX, currentY)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (sensorManager != null && activeSensor != null) {
                    sensorManager.registerListener(listener, activeSensor, SensorManager.SENSOR_DELAY_UI)
                }
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                sensorManager?.unregisterListener(listener)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            sensorManager?.unregisterListener(listener)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    return tiltState
}
