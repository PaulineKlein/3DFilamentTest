package com.pklein.filamenttest

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.Choreographer
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.filament.utils.*
import java.nio.ByteBuffer

class MainActivity : AppCompatActivity(), View.OnTouchListener {

    private lateinit var surfaceView: SurfaceView
    private lateinit var choreographer: Choreographer
    private lateinit var modelViewer: ModelViewer

    // statique
    private val frameCallbackStatic = object : Choreographer.FrameCallback {
        override fun doFrame(currentTime: Long) {
            choreographer.postFrameCallback(this)
            modelViewer.render(currentTime)
        }
    }

    // dynamique :
    private val frameCallback3 = object : Choreographer.FrameCallback {
        private val startTime = System.nanoTime()
        override fun doFrame(currentTime: Long) {
            val seconds = (currentTime - startTime).toDouble() / 1_000_000_000
            choreographer.postFrameCallback(this)
            modelViewer.animator?.apply {
                if (animationCount > 0) {
                    // This takes two arguments: an index into the model’s list of animation
                    // definitions, and the the elapsed time for that particular animation.
                    applyAnimation(0, seconds.toFloat())
                }
                updateBoneMatrices()
            }
            modelViewer.render(currentTime)
        }
    }

    // let’s modify the application to make the drone continuously spin around the Z axis
    private val frameCallback = object : Choreographer.FrameCallback {
        private val startTime = System.nanoTime()
        override fun doFrame(currentTime: Long) {
            val seconds = (currentTime - startTime).toDouble() / 1_000_000_000
            choreographer.postFrameCallback(this)
            // Reset the root transform, then rotate it around the Z axis.
            modelViewer.asset?.apply {
                modelViewer.transformToUnitCube()
                val rootTransform = this.root.getTransform()
                val degrees = 20f * seconds.toFloat()
                val zAxis = Float3(0f, 0f, 1f)
                this.root.setTransform(rootTransform * rotation(zAxis, degrees))
            }
            modelViewer.render(currentTime)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        surfaceView = SurfaceView(this).apply { setContentView(this) }
        choreographer = Choreographer.getInstance()
        modelViewer = ModelViewer(surfaceView)
        surfaceView.setOnTouchListener(modelViewer)
        //  surfaceView.setOnTouchListener(this)

        // load object
        loadGltf("BusterDrone")
        // loadGlb("DamagedHelmet")
        // loadGlb("Vehicle")

        // load Landscape
        loadEnvironment("venetian_crossroads_2k")
        // modelViewer.scene.skybox = Skybox.Builder().build(modelViewer.engine)
        // modelViewer.scene.skybox = Skybox.Builder().color(0.1f, 0.2f, 0.4f, 1.0f).build(modelViewer.engine)
        // modelViewer.camera.lookAt(3.0, 2.0, 3.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0)
        hideFloor()
    }

    override fun onResume() {
        super.onResume()
        choreographer.postFrameCallback(frameCallback)
        // choreographer.postFrameCallback(frameCallbackStatic)
    }

    override fun onPause() {
        super.onPause()
        choreographer.removeFrameCallback(frameCallbackStatic)
    }

    override fun onDestroy() {
        super.onDestroy()
        choreographer.removeFrameCallback(frameCallbackStatic)
    }

    private fun loadGlb(name: String) {
        // read the contents of the glb file into a ByteBuffer and pass it to ModelViewer
        val buffer = readAsset("models/${name}.glb")
        modelViewer.loadModelGlb(buffer)
        // This tells the model viewer to transform the root node of the scene such
        // that it fits into a 1x1x1 cube centered at the origin.
        modelViewer.transformToUnitCube()
    }

    private fun loadGltf(name: String) {
        val buffer = readAsset("models/${name}.gltf")
        modelViewer.loadModelGltf(buffer) { uri -> readAsset("models/$uri") }
        modelViewer.transformToUnitCube()
    }

    private fun readAsset(assetName: String): ByteBuffer {
        val input = assets.open(assetName)
        val bytes = ByteArray(input.available())
        input.read(bytes)
        return ByteBuffer.wrap(bytes)
    }

    private fun loadEnvironment(ibl: String) {
        // Create the indirect light source and add it to the scene.
        var buffer = readAsset("envs/$ibl/${ibl}_ibl.ktx")
        KtxLoader.createIndirectLight(modelViewer.engine, buffer).apply {
            intensity = 50_000f
            modelViewer.scene.indirectLight = this
        }

        // Create the sky box and add it to the scene.
        buffer = readAsset("envs/$ibl/${ibl}_skybox.ktx")
        KtxLoader.createSkybox(modelViewer.engine, buffer).apply {
            modelViewer.scene.skybox = this
        }
    }

    private fun Int.getTransform(): Mat4 {
        val tm = modelViewer.engine.transformManager
        return Mat4.of(*tm.getTransform(tm.getInstance(this), null))
    }

    private fun Int.setTransform(mat: Mat4) {
        val tm = modelViewer.engine.transformManager
        tm.setTransform(tm.getInstance(this), mat.toFloatArray())
    }

    private fun hideFloor() {
        // let’s try hiding the floor disk and disabling the emissive tail lights on the back of the drone
        val asset = modelViewer.asset!!
        val rm = modelViewer.engine.renderableManager
        for (entity in asset.entities) {
            val renderable = rm.getInstance(entity)
            if (renderable == 0) {
                // Some of the entities in the asset do not have a renderable component
                // so we check for zero at the top of the loop.
                continue
            }
            if (asset.getName(entity) == "Scheibe_Boden_0") {
                rm.setLayerMask(renderable, 0xff, 0x00) // hide it from the view
                // method takes two bitmasks: a list of bits to affect, and the replacement values
                // for those bits. In this case we want to hide the renderable from everything so we’re
                // setting all visibility bits to zero.
            }
            val material = rm.getMaterialInstanceAt(renderable, 0)
            material.setParameter("emissiveFactor", 0f, 0f, 0f)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                Log.e("onTouch", "ACTION_DOWN")
                choreographer.postFrameCallback(frameCallbackStatic)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                Log.e("onTouch", "ACTION_MOVE")
                choreographer.postFrameCallback(frameCallbackStatic)
                return true
            }
            MotionEvent.ACTION_UP -> {
                Log.e("onTouch", "ACTION_UP")
                choreographer.postFrameCallback(frameCallbackStatic)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    companion object {
        init {
            Utils.init()
        }
    }
}