package com.pklein.filamenttest.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.MutableLiveData
import kotlin.math.round

class SensorModel(private val sensorManager: SensorManager) : SensorEventListener {
    private var accelerometer: Sensor? = null
    private var magneto: Sensor? = null

    private val accelerometerRead = FloatArray(3)
    private val magnetometerRead = FloatArray(3)

    // These two arrays will hold the values of the rotation matrix and orientation angles.
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    // the angle calculated in degrees :
    val degreeCalculated = MutableLiveData<Float>()

    init {
        // we need to check whether the sensors we need are available on the device or not
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            this.accelerometer = it
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.let {
            this.magneto = it
        }
    }

    fun registerListener() {
        accelerometer?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
        magneto?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun unregisterListener() {
        sensorManager.unregisterListener(this)
    }

    private fun updateOrientationAngles() {
        // To find the device’s orientation, you first need to determine its rotation matrix.
        // A rotation matrix helps map points from the device’s coordinate system to the real-world coordinate system.
        SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerRead, magnetometerRead)
        // It then uses that rotation matrix, which consists of an array of nine values, and maps it to a usable matrix with three values.
        // In the variable orientation, you get values (in radians) that represent
        // orientation[0] = Azimuth (rotation around the z-axis)
        // orientation[1] = Pitch (rotation around the x-axis)
        // orientation[2] = Roll (rotation around the y-axis)
        val orientation = SensorManager.getOrientation(rotationMatrix, orientationAngles)
        // Next, it converts the azimuth to degrees, adding 360 because the angle is always positive
        val degreesAngle = (Math.toDegrees(orientation[0].toDouble()) + 360.0) % 360.0
        // Finally, it rounds the angle up to two decimal places.
        val angle = (round(degreesAngle * 100) / 100).toFloat()
        degreeCalculated.postValue(angle)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        // a sensor reports a new value
        when (event?.sensor?.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                // System.arrayCopy copies values from the sensors into its respective array.
                if(event.values != null) {
                    System.arraycopy(event.values, 0, accelerometerRead, 0, accelerometerRead.size)
                }
                updateOrientationAngles()
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                if(event.values != null) {
                    System.arraycopy(event.values, 0, magnetometerRead, 0, magnetometerRead.size)
                }
                updateOrientationAngles()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // a sensor’s accuracy changes. In this case, the system invokes the onAccuracyChanged()
        // method, providing you with a reference to the Sensor object, which has changed,
        // and the new accuracy of the sensor.
    }
}