package com.pklein.filamenttest.filament

import android.view.Choreographer
import com.google.android.filament.Skybox
import com.google.android.filament.utils.Float3
import com.google.android.filament.utils.ModelViewer
import com.google.android.filament.utils.rotation
import com.pklein.filamenttest.getTransform
import com.pklein.filamenttest.setTransform

const val VEHICLE_OBJECT = "Vehicle"
const val HELMET_OBJECT = "DamagedHelmet"
const val DRONE_OBJECT = "BusterDrone"
const val SCENE = "venetian_crossroads_2k"

class FilamentRepository(
    private val modelViewer: ModelViewer,
    private val choreographer: Choreographer
) {

    private val frameCallbackStatic = object : Choreographer.FrameCallback {
        override fun doFrame(currentTime: Long) {
            choreographer.postFrameCallback(this)
            modelViewer.render(currentTime)
        }
    }
    private val frameCallbackDynamic = object : Choreographer.FrameCallback {
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
    private val frameCallbackSpin = object : Choreographer.FrameCallback {
        private val startTime = System.nanoTime()
        override fun doFrame(currentTime: Long) {
            val seconds = (currentTime - startTime).toDouble() / 1_000_000_000
            choreographer.postFrameCallback(this)
            // Reset the root transform, then rotate it around the Z axis.
            modelViewer.asset?.apply {
                modelViewer.transformToUnitCube()
                val rootTransform = this.root.getTransform(modelViewer)
                val degrees = 20f * seconds.toFloat()
                val zAxis = Float3(0f, 0f, 1f)
                // make the drone continuously spin around the Z axis
                this.root.setTransform(rootTransform * rotation(zAxis, degrees), modelViewer)
            }
            modelViewer.render(currentTime)
        }
    }

    val vehicleScene = FilamentScene(
        VEHICLE_OBJECT,
        TypeFile.GLB,
        null,
        Skybox.Builder().color(0.1f, 0.2f, 0.4f, 1.0f).build(modelViewer.engine),
        frameCallbackStatic
    )

    val busterDrone = FilamentScene(
        DRONE_OBJECT,
        TypeFile.GLTF,
        SCENE,
        Skybox.Builder().build(modelViewer.engine),
        frameCallbackSpin
    )

    val damagedHelmet = FilamentScene(
        HELMET_OBJECT,
        TypeFile.GLB,
        SCENE,
        Skybox.Builder().build(modelViewer.engine),
        frameCallbackDynamic
    )
}