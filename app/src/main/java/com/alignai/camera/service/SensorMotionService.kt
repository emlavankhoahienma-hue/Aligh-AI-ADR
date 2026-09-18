package com.alignai.camera.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.alignai.camera.domain.SpatialTrackingEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

class SensorMotionService(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val gyroscope = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var lastGyroTimestamp: Long = 0

    private val _rollDegrees = MutableStateFlow(0f)
    val rollDegrees: StateFlow<Float> = _rollDegrees.asStateFlow()

    private val _isLevel = MutableStateFlow(true)
    val isLevel: StateFlow<Boolean> = _isLevel.asStateFlow()

    fun start() {
        sensorManager?.let { sm ->
            gyroscope?.let { gyro ->
                sm.registerListener(this, gyro, SensorManager.SENSOR_DELAY_GAME)
            }
            rotationSensor?.let { rot ->
                sm.registerListener(this, rot, SensorManager.SENSOR_DELAY_UI)
            }
        }
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
        lastGyroTimestamp = 0
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        when (event.sensor.type) {
            Sensor.TYPE_GYROSCOPE -> {
                val now = event.timestamp
                if (lastGyroTimestamp != 0L) {
                    val dt = (now - lastGyroTimestamp) * 1e-9f // Convert ns to seconds
                    val rateX = event.values[0]
                    val rateY = event.values[1]
                    SpatialTrackingEngine.shared.updateWithGyroscope(rateX, rateY, dt)
                }
                lastGyroTimestamp = now
            }
            Sensor.TYPE_ROTATION_VECTOR -> {
                val rotationMatrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                val orientation = FloatArray(3)
                SensorManager.getOrientation(rotationMatrix, orientation)
                // Roll is rotation around Y axis: orientation[2] in radians
                val rollDeg = (orientation[2] * 180.0f / Math.PI.toFloat())
                _rollDegrees.value = rollDeg
                _isLevel.value = abs(rollDeg) < 1.5f
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val ax = event.values[0]
                val ay = event.values[1]
                val rollDeg = (Math.atan2(ax.toDouble(), ay.toDouble()) * 180.0 / Math.PI).toFloat()
                _rollDegrees.value = rollDeg
                _isLevel.value = abs(rollDeg) < 1.5f
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
