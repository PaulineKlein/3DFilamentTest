package com.pklein.filamenttest

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.SensorManager
import android.os.Bundle
import android.view.SurfaceView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.filament.utils.Utils
import com.pklein.filamenttest.filament.FilamentModel
import com.pklein.filamenttest.sensor.SensorModel

class MainActivity : AppCompatActivity() {

    private lateinit var surfaceView: SurfaceView
    private lateinit var filamentModel: FilamentModel
    private lateinit var sensorModel: SensorModel

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Manage 3D Scene
        surfaceView = SurfaceView(this).apply { setContentView(this) }
        filamentModel = FilamentModel(surfaceView, assets)
        // Manage Sensors :
        sensorModel = SensorModel(getSystemService(Context.SENSOR_SERVICE) as SensorManager)
        sensorModel.degreeCalculated.observe(this, { angle ->
            filamentModel.updateObjectPosition(angle)
        })
    }

    override fun onResume() {
        super.onResume()
        filamentModel.postFrameCallback()
        sensorModel.registerListener()
    }

    override fun onPause() {
        super.onPause()
        filamentModel.removeFrameCallback()
        sensorModel.unregisterListener()
    }

    override fun onDestroy() {
        super.onDestroy()
        filamentModel.removeFrameCallback()
        sensorModel.unregisterListener()
    }

    companion object {
        init {
            // Load Filament
            Utils.init()
        }
    }
}