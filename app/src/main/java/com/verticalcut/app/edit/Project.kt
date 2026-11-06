package com.verticalcut.app.edit

import android.net.Uri
import java.util.UUID

/** Un fragmento de video en la línea de tiempo. startMs/endMs son relativos al archivo original. */
data class Clip(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val sourceDurationMs: Long,
    val startMs: Long = 0L,
    val endMs: Long = sourceDurationMs,
) {
    val durationMs: Long get() = (endMs - startMs).coerceAtLeast(0L)
}

/** Texto que se dibuja sobre el video durante toda su duración. */
data class TextOverlay(
    val text: String = "",
    val colorArgb: Int = 0xFFFFFFFF.toInt(),
    val sizeSp: Int = 42,
)

/** Pista de música de fondo. */
data class MusicTrack(
    val uri: Uri,
    val name: String,
    val volume: Float = 0.8f,
)

enum class Filter(val label: String) {
    NONE("Original"),
    WARM("Cálido"),
    COOL("Frío"),
    VIVID("Vivo"),
    MONO("Blanco y negro"),
    NEGATIVE("Negativo"),
}

data class Project(
    val clips: List<Clip> = emptyList(),
    val filter: Filter = Filter.NONE,
    val text: TextOverlay? = null,
    val music: MusicTrack? = null,
) {
    val totalDurationMs: Long get() = clips.sumOf { it.durationMs }

    /** Dado un instante global de la línea de tiempo, devuelve (índice del clip, offset dentro del clip). */
    fun locate(globalMs: Long): Pair<Int, Long>? {
        var acc = 0L
        clips.forEachIndexed { i, c ->
            if (globalMs < acc + c.durationMs) return i to (globalMs - acc)
            acc += c.durationMs
        }
        return null
    }

    /** Instante global donde empieza el clip en la posición [index]. */
    fun startOf(index: Int): Long = clips.take(index).sumOf { it.durationMs }
}
