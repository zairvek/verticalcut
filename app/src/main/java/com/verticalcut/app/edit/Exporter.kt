package com.verticalcut.app.edit

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.TextOverlay as Media3TextOverlay
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.google.common.collect.ImmutableList
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Renderiza el proyecto a un MP4 vertical 1080x1920 con Media3 Transformer y lo guarda en Movies/VerticalCut.
 */
@OptIn(UnstableApi::class)
class Exporter(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())

    fun export(
        project: Project,
        onProgress: (Int) -> Unit,
        onDone: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
        val outDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val outFile = File(outDir, "VerticalCut_$stamp.mp4")

        val composition = buildComposition(project)

        val transformer = Transformer.Builder(context)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    runCatching { saveToGallery(outFile) }
                        .onSuccess { onDone(it) }
                        .onFailure { onError("No se pudo guardar: ${it.message}") }
                }

                override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                    onError(exportException.message ?: "Error al exportar")
                }
            })
            .build()

        transformer.start(composition, outFile.absolutePath)

        val holder = ProgressHolder()
        val poll = object : Runnable {
            override fun run() {
                val state = transformer.getProgress(holder)
                if (state == Transformer.PROGRESS_STATE_AVAILABLE) onProgress(holder.progress)
                if (state != Transformer.PROGRESS_STATE_NOT_STARTED) mainHandler.postDelayed(this, 300)
            }
        }
        mainHandler.post(poll)
    }

    private fun buildComposition(project: Project): Composition {
        // Efectos de video aplicados a todo el proyecto: formato vertical + filtro + texto
        val videoEffects = mutableListOf<Effect>()
        videoEffects += Presentation.createForAspectRatio(9f / 16f, Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP)
        videoEffects += project.filter.toEffects()
        project.text?.let { videoEffects += textEffect(it) }

        val videoItems = project.clips.map { clip ->
            val item = MediaItem.Builder()
                .setUri(clip.uri)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(clip.startMs)
                        .setEndPositionMs(clip.endMs)
                        .build()
                )
                .build()
            EditedMediaItem.Builder(item).build()
        }
        val videoSequence = EditedMediaItemSequence(videoItems, /* isLooping = */ false)

        val sequences = mutableListOf(videoSequence)
        project.music?.let { music ->
            val musicItem = EditedMediaItem.Builder(MediaItem.fromUri(music.uri))
                .setRemoveVideo(true)
                .build()
            // En bucle: se corta sola al terminar la secuencia de video
            sequences += EditedMediaItemSequence(listOf(musicItem), /* isLooping = */ true)
        }

        return Composition.Builder(sequences)
            .setEffects(Effects(/* audioProcessors = */ emptyList(), videoEffects))
            .build()
    }

    private fun textEffect(t: TextOverlay): Effect {
        val span = SpannableString(t.text)
        span.setSpan(ForegroundColorSpan(t.colorArgb), 0, span.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        // Tamaño en píxeles sobre un lienzo de 1080x1920
        span.setSpan(AbsoluteSizeSpan(t.sizeSp * 3), 0, span.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        val overlay = Media3TextOverlay.createStaticTextOverlay(span)
        return OverlayEffect(ImmutableList.of(overlay))
    }

    /** Copia el archivo a la galería (Movies/VerticalCut) y devuelve una descripción legible de dónde quedó. */
    private fun saveToGallery(file: File): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/VerticalCut")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("MediaStore no devolvió URI")
            resolver.openOutputStream(uri)!!.use { out -> file.inputStream().use { it.copyTo(out) } }
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            file.delete()
            return "Galería › Movies/VerticalCut/${file.name}"
        } else {
            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "VerticalCut").apply { mkdirs() }
            val dest = File(dir, file.name)
            file.copyTo(dest, overwrite = true)
            file.delete()
            return dest.absolutePath
        }
    }
}
