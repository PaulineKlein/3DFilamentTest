package com.pklein.filamenttest

import com.google.android.filament.utils.Mat4
import com.google.android.filament.utils.ModelViewer

fun Int.getTransform(modelViewer: ModelViewer): Mat4 {
    val tm = modelViewer.engine.transformManager
    val outLocalTransform: FloatArray? = null
    return Mat4.of(*tm.getTransform(tm.getInstance(this), outLocalTransform))
}

fun Int.setTransform(mat: Mat4, modelViewer: ModelViewer) {
    val tm = modelViewer.engine.transformManager
    tm.setTransform(tm.getInstance(this), mat.toFloatArray())
}