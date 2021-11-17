package com.pklein.filamenttest.filament

import android.view.Choreographer
import com.google.android.filament.Skybox

enum class TypeFile { GLB, GLTF }

data class FilamentScene(
    val objectName: String,
    val typeFile: TypeFile,
    val environmentName: String?,
    val skybox: Skybox,
    val frameCallback: Choreographer.FrameCallback
)
