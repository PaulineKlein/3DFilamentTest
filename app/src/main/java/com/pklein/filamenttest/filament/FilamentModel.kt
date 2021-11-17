package com.pklein.filamenttest.filament

import android.annotation.SuppressLint
import android.content.res.AssetManager
import android.view.Choreographer
import android.view.SurfaceView
import com.google.android.filament.utils.Float3
import com.google.android.filament.utils.KTXLoader
import com.google.android.filament.utils.ModelViewer
import com.google.android.filament.utils.rotation
import com.pklein.filamenttest.getTransform
import com.pklein.filamenttest.setTransform
import java.nio.ByteBuffer

@SuppressLint("ClickableViewAccessibility")
class FilamentModel(private val surfaceView: SurfaceView, private val assets: AssetManager) {

    private val choreographer: Choreographer = Choreographer.getInstance()
    private val modelViewer: ModelViewer = ModelViewer(surfaceView)
    private val filamentRepository = FilamentRepository(modelViewer, choreographer)
    private val myScene: FilamentScene

    init {
        surfaceView.setOnTouchListener(modelViewer)
        myScene = filamentRepository.vehicleScene

        // load object
        if (myScene.typeFile == TypeFile.GLTF) {
            loadGltf(myScene.objectName)
        } else {
            loadGlb(myScene.objectName)
        }

        // load Landscape
        if (!myScene.environmentName.isNullOrEmpty()) {
            loadEnvironment(myScene.environmentName.toString())
        }
        modelViewer.scene.skybox = myScene.skybox
        if (myScene.objectName == DRONE_OBJECT) {
            hideFloor()
        }
    }

    fun postFrameCallback() {
        choreographer.postFrameCallback(myScene.frameCallback)
    }

    fun removeFrameCallback() {
        choreographer.removeFrameCallback(myScene.frameCallback)
    }

    fun updateObjectPosition(angle: Float) {
        modelViewer.asset?.apply {
            modelViewer.transformToUnitCube()
            val rootTransform = this.root.getTransform(modelViewer)
            val zAxis = Float3(0f, 0f, 1f)
            this.root.setTransform(rootTransform * rotation(zAxis * angle), modelViewer)
        }
    }

    private fun readAsset(assetName: String): ByteBuffer {
        val input = assets.open(assetName)
        val bytes = ByteArray(input.available())
        input.read(bytes)
        return ByteBuffer.wrap(bytes)
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

    private fun loadEnvironment(ibl: String) {
        // Create the indirect light source and add it to the scene.
        var buffer = readAsset("envs/$ibl/${ibl}_ibl.ktx")
        KTXLoader.createIndirectLight(modelViewer.engine, buffer).apply {
            intensity = 50_000f
            modelViewer.scene.indirectLight = this
        }
        // Create the sky box and add it to the scene.
        buffer = readAsset("envs/$ibl/${ibl}_skybox.ktx")
        KTXLoader.createSkybox(modelViewer.engine, buffer).apply {
            modelViewer.scene.skybox = this
        }
    }

    private fun hideFloor() {
        // hide the floor disk and disabling the emissive tail lights on the back of the drone
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
}