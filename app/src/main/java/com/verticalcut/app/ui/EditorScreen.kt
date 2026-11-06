package com.verticalcut.app.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.verticalcut.app.edit.Clip
import com.verticalcut.app.edit.EditorViewModel
import com.verticalcut.app.edit.ExportState
import com.verticalcut.app.edit.Filter
import com.verticalcut.app.edit.TextOverlay
import com.verticalcut.app.edit.toEffects
import kotlinx.coroutines.delay

private enum class Tool { NONE, TRIM, TEXT, MUSIC, FILTER }

private val PX_PER_SEC = 44.dp

@OptIn(UnstableApi::class)
@Composable
fun EditorScreen(vm: EditorViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val project by vm.project.collectAsStateWithLifecycle()
    val selectedId by vm.selectedClipId.collectAsStateWithLifecycle()
    val version by vm.timelineVersion.collectAsStateWithLifecycle()
    val export by vm.export.collectAsStateWithLifecycle()

    var tool by remember { mutableStateOf(Tool.NONE) }
    var isPlaying by remember { mutableStateOf(false) }
    var positionMs by remember { mutableLongStateOf(0L) }

    // Reproductor de video (vista previa) y reproductor de música
    val exo = remember { ExoPlayer.Builder(context).build() }
    val musicPlayer = remember { ExoPlayer.Builder(context).build().apply { repeatMode = Player.REPEAT_MODE_ALL } }
    DisposableEffect(Unit) {
        onDispose { exo.release(); musicPlayer.release() }
    }

    // Recargar la línea de tiempo cada vez que cambian los clips, el filtro o la música
    LaunchedEffect(version) {
        val wasPlaying = exo.isPlaying
        val keep = positionMs
        exo.stop()
        // Si el dispositivo no soporta efectos en la vista previa, se ven igualmente al exportar
        runCatching { exo.setVideoEffects(project.filter.toEffects()) }
        exo.setMediaItems(project.clips.map { it.toMediaItem() })
        exo.prepare()
        seekGlobal(exo, project.clips, keep)
        exo.playWhenReady = wasPlaying

        musicPlayer.stop()
        project.music?.let {
            musicPlayer.setMediaItem(MediaItem.fromUri(it.uri))
            musicPlayer.prepare()
            musicPlayer.volume = it.volume
            musicPlayer.playWhenReady = wasPlaying
        }
    }
    LaunchedEffect(project.music?.volume) { musicPlayer.volume = project.music?.volume ?: 1f }

    // Seguir la posición de reproducción
    LaunchedEffect(Unit) {
        while (true) {
            val idx = exo.currentMediaItemIndex
            if (project.clips.isNotEmpty() && idx in project.clips.indices) {
                positionMs = project.startOf(idx) + exo.currentPosition
            }
            isPlaying = exo.isPlaying
            delay(80)
        }
    }

    val musicPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        vm.setMusic(uri)
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
        if (uris.isNotEmpty()) vm.addClips(uris)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Barra superior
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { exo.pause(); musicPlayer.pause(); onBack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = TextMain)
            }
            Text("VerticalCut", color = TextMain, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Button(
                onClick = { exo.pause(); musicPlayer.pause(); vm.exportProject() },
                enabled = project.clips.isNotEmpty() && export !is ExportState.Running,
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg),
                shape = RoundedCornerShape(10.dp),
            ) { Text("Exportar", fontWeight = FontWeight.SemiBold) }
        }

        // Vista previa 9:16
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .aspectRatio(9f / 16f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = exo
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
                project.text?.let { t ->
                    Text(
                        t.text,
                        color = Color(t.colorArgb),
                        fontSize = (t.sizeSp * 0.45f).sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center).padding(12.dp),
                    )
                }
            }
        }

        // Controles de reproducción
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = {
                if (exo.isPlaying) { exo.pause(); musicPlayer.pause() } else {
                    if (exo.playbackState == Player.STATE_ENDED) { exo.seekTo(0, 0); musicPlayer.seekTo(0) }
                    exo.play(); if (project.music != null) musicPlayer.play()
                }
            }) {
                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, tint = TextMain)
            }
            Text(
                "${fmt(positionMs)} / ${fmt(project.totalDurationMs)}",
                color = TextDim, fontSize = 13.sp, modifier = Modifier.weight(1f),
            )
            project.music?.let { Text("♪ ${it.name}", color = Accent, fontSize = 12.sp, maxLines = 1) }
        }

        // Línea de tiempo
        Timeline(
            clips = project.clips,
            selectedId = selectedId,
            positionMs = positionMs,
            onSelect = { vm.select(it) },
            onSeek = { ms -> seekGlobal(exo, project.clips, ms); positionMs = ms },
            onAdd = { videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) },
        )

        // Panel de la herramienta activa
        val selected = project.clips.firstOrNull { it.id == selectedId }
        when (tool) {
            Tool.TRIM -> selected?.let { clip ->
                ToolPanel("Recortar clip", onClose = { tool = Tool.NONE }) {
                    var range by remember(clip.id) { mutableStateOf(clip.startMs.toFloat()..clip.endMs.toFloat()) }
                    Text("${fmt(range.start.toLong())} – ${fmt(range.endInclusive.toLong())}", color = TextDim, fontSize = 12.sp)
                    RangeSlider(
                        value = range,
                        onValueChange = { range = it },
                        onValueChangeFinished = { vm.trimSelected(range.start.toLong(), range.endInclusive.toLong()) },
                        valueRange = 0f..clip.sourceDurationMs.toFloat(),
                    )
                }
            }
            Tool.FILTER -> ToolPanel("Filtros", onClose = { tool = Tool.NONE }) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Filter.entries.forEach { f ->
                        val on = project.filter == f
                        Text(
                            f.label,
                            color = if (on) Bg else TextMain,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (on) Accent else SurfaceHigh)
                                .clickable { vm.setFilter(f) }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
            }
            Tool.MUSIC -> ToolPanel("Música", onClose = { tool = Tool.NONE }) {
                val m = project.music
                if (m == null) {
                    Button(onClick = { musicPicker.launch(arrayOf("audio/*")) }, shape = RoundedCornerShape(10.dp)) {
                        Text("Elegir canción")
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(m.name, color = TextMain, modifier = Modifier.weight(1f), maxLines = 1)
                        TextButton(onClick = { vm.setMusic(null) }) { Text("Quitar", color = Accent2) }
                    }
                    Text("Volumen", color = TextDim, fontSize = 12.sp)
                    Slider(value = m.volume, onValueChange = { vm.setMusicVolume(it) })
                }
            }
            Tool.TEXT -> {
                var draft by remember { mutableStateOf(project.text?.text ?: "") }
                AlertDialog(
                    onDismissRequest = { tool = Tool.NONE },
                    confirmButton = {
                        TextButton(onClick = { vm.setText(TextOverlay(text = draft)); tool = Tool.NONE }) { Text("Aplicar") }
                    },
                    dismissButton = {
                        TextButton(onClick = { vm.setText(null); tool = Tool.NONE }) { Text("Quitar texto") }
                    },
                    title = { Text("Texto sobre el video") },
                    text = { OutlinedTextField(value = draft, onValueChange = { draft = it }, singleLine = false) },
                )
            }
            Tool.NONE -> Unit
        }

        // Barra de herramientas
        Row(
            Modifier
                .fillMaxWidth()
                .background(Surface)
                .padding(vertical = 6.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ToolButton(Icons.Default.ContentCut, "Dividir") { vm.splitAt(positionMs) }
            ToolButton(Icons.Default.Crop, "Recortar") { tool = Tool.TRIM }
            ToolButton(Icons.Default.TextFields, "Texto") { tool = Tool.TEXT }
            ToolButton(Icons.Default.MusicNote, "Música") { tool = Tool.MUSIC }
            ToolButton(Icons.Default.Tune, "Filtros") { tool = Tool.FILTER }
            ToolButton(Icons.Default.ArrowBack, "Atrás") { vm.moveSelected(-1) }
            ToolButton(Icons.Default.ArrowForward, "Adelante") { vm.moveSelected(+1) }
            ToolButton(Icons.Default.Delete, "Eliminar", tint = Accent2) { vm.deleteSelected() }
        }
    }

    // Estado de exportación
    when (val e = export) {
        is ExportState.Running -> AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text("Exportando…") },
            text = {
                Column {
                    LinearProgressIndicator(progress = { e.progress / 100f }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Text("${e.progress}% · 1080×1920", color = TextDim)
                }
            },
        )
        is ExportState.Done -> AlertDialog(
            onDismissRequest = { vm.resetExport() },
            confirmButton = { TextButton(onClick = { vm.resetExport() }) { Text("Listo") } },
            title = { Text("Video guardado") },
            text = { Text(e.savedTo) },
        )
        is ExportState.Failed -> AlertDialog(
            onDismissRequest = { vm.resetExport() },
            confirmButton = { TextButton(onClick = { vm.resetExport() }) { Text("Cerrar") } },
            title = { Text("No se pudo exportar") },
            text = { Text(e.message) },
        )
        ExportState.Idle -> Unit
    }
}

@Composable
private fun Timeline(
    clips: List<Clip>,
    selectedId: String?,
    positionMs: Long,
    onSelect: (String) -> Unit,
    onSeek: (Long) -> Unit,
    onAdd: () -> Unit,
) {
    val density = LocalDensity.current
    val pxPerSecDp = PX_PER_SEC
    val pxPerSecPx = with(density) { pxPerSecDp.toPx() }
    val scroll = rememberScrollState()
    val context = LocalContext.current
    val clipStarts = remember(clips) {
        var acc = 0L
        clips.map { c -> acc.also { acc += c.durationMs } }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .height(84.dp)
            .background(Bg)
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .horizontalScroll(scroll)
                .padding(start = 16.dp, end = 80.dp, top = 10.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            clips.forEachIndexed { index, clip ->
                val widthDp = pxPerSecDp * (clip.durationMs / 1000f)
                val thumbs = (clip.durationMs / 1000f).let { kotlin.math.ceil(it).toInt().coerceAtLeast(1) }
                val isSel = clip.id == selectedId
                Row(
                    Modifier
                        .width(widthDp.coerceAtLeast(24.dp))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(6.dp))
                        .border(2.dp, if (isSel) Accent else Color.Transparent, RoundedCornerShape(6.dp))
                        .pointerInput(clip.id, index) {
                            detectTapGestures { pos ->
                                onSelect(clip.id)
                                val offsetMs = (pos.x / pxPerSecPx * 1000f).toLong().coerceIn(0L, clip.durationMs - 1)
                                onSeek(clipStarts[index] + offsetMs)
                            }
                        },
                ) {
                    repeat(thumbs) { i ->
                        val req = ImageRequest.Builder(context)
                            .data(clip.uri)
                            .videoFrameMillis(clip.startMs + i * 1000L)
                            .decoderFactory(VideoFrameDecoder.Factory())
                            .build()
                        AsyncImage(
                            model = req,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.width(pxPerSecDp).fillMaxHeight(),
                        )
                    }
                }
            }
            Box(
                Modifier
                    .width(44.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceHigh)
                    .clickable { onAdd() },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Default.Add, contentDescription = "Añadir clip", tint = Accent) }
        }

        // Cabezal de reproducción
        val headX = 16.dp + pxPerSecDp * (positionMs / 1000f)
        val scrollDp = with(density) { scroll.value.toDp() }
        Box(
            Modifier
                .offset(x = headX - scrollDp - 1.dp)
                .width(2.dp)
                .fillMaxHeight()
                .background(Accent2)
        )

    }
    // Mantener el cabezal a la vista mientras avanza la reproducción
    LaunchedEffect(positionMs) {
        // Mantener el cabezal visible
        val headPx = with(density) { (16.dp + pxPerSecDp * (positionMs / 1000f)).toPx() }
        val viewport = scroll.viewportSize
        if (viewport > 0 && (headPx < scroll.value || headPx > scroll.value + viewport * 0.85f)) {
            scroll.scrollTo((headPx - viewport * 0.3f).toInt().coerceAtLeast(0))
        }
    }
}

@Composable
private fun ToolPanel(title: String, onClose: () -> Unit, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Surface)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = TextMain, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TextDim) }
        }
        content()
    }
}

@Composable
private fun ToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = TextMain,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(24.dp))
        Text(label, color = tint, fontSize = 11.sp)
    }
}

private fun Clip.toMediaItem(): MediaItem = MediaItem.Builder()
    .setUri(uri)
    .setClippingConfiguration(
        MediaItem.ClippingConfiguration.Builder()
            .setStartPositionMs(startMs)
            .setEndPositionMs(endMs)
            .build()
    )
    .build()

private fun seekGlobal(player: ExoPlayer, clips: List<Clip>, globalMs: Long) {
    var acc = 0L
    clips.forEachIndexed { i, c ->
        if (globalMs < acc + c.durationMs) { player.seekTo(i, globalMs - acc); return }
        acc += c.durationMs
    }
    if (clips.isNotEmpty()) player.seekTo(clips.lastIndex, clips.last().durationMs)
}

private fun fmt(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d.%d".format(s / 60, s % 60, (ms % 1000) / 100)
}
