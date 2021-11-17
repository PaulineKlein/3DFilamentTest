package com.pklein.filamenttest

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.mock
import com.pklein.filamenttest.sensor.SensorModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner


@RunWith(MockitoJUnitRunner::class)
class SensorModelTest {

    private val eventValues: FloatArray = floatArrayOf(0f, 0f, 0f)
    private val observer: Observer<Float> = mock()
    private val sensorManager = mockk<SensorManager>(relaxUnitFun = true)
    private val sensor = mockk<Sensor>(relaxUnitFun = true)
    private val event = mockk<SensorEvent>(relaxUnitFun = true)
    private lateinit var sensorModel: SensorModel

    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    @Before
    fun init() {
        event.sensor = sensor
        every {
            event.sensor?.type
        } returns Sensor.TYPE_ACCELEROMETER
        every {
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        } returns sensor
        every {
            sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        } returns sensor
        mockkStatic(SensorManager::class)
        every {
            SensorManager.getRotationMatrix(any(), null, any(), any())
        } returns true
        every {
            SensorManager.getOrientation(any(), any())
        } returns eventValues

        sensorModel = SensorModel(sensorManager)
        sensorModel.degreeCalculated.observeForever(observer)
        every {
            sensorManager.registerListener(sensorModel, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        } returns true
    }

    @Test
    fun `updateOrientationAngles when receive listener should return 0`() {
        sensorModel.registerListener()
        sensorModel.onSensorChanged(event)
        assertEquals(0f, sensorModel.degreeCalculated.value)
    }
}