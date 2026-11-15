package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import kotlin.math.abs

class TourTargetRegistry {
    private val _targets = mutableStateMapOf<String, Rect>()
    val targets: Map<String, Rect> get() = _targets

    fun update(id: String, rect: Rect) {
        if (rect.width <= 1f || rect.height <= 1f) return
        val prev = _targets[id]
        if (prev == null ||
            abs(prev.left - rect.left) > 1.5f ||
            abs(prev.top - rect.top) > 1.5f ||
            abs(prev.right - rect.right) > 1.5f ||
            abs(prev.bottom - rect.bottom) > 1.5f
        ) {
            _targets[id] = rect
        }
    }

    fun get(id: String): Rect? = _targets[id]
}

val LocalTourRegistry = staticCompositionLocalOf<TourTargetRegistry?> { null }

@Composable
fun rememberTourTargetRegistry(): TourTargetRegistry = remember { TourTargetRegistry() }

fun Modifier.tourTarget(id: String): Modifier = composed {
    val registry = LocalTourRegistry.current
    if (registry == null) {
        this
    } else {
        this.onGloballyPositioned { coords ->
            if (coords.isAttached) {
                registry.update(id, coords.boundsInRoot())
            }
        }
    }
}
