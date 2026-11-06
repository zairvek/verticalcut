package com.verticalcut.app.edit

import android.app.Application
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class ExportState {
    data object Idle : ExportState()
    data class Running(val progress: Int) : ExportState()
    data class Done(val savedTo: String) : ExportState()
    data class Failed(val message: String) : ExportState()
}

class EditorViewModel(app: Application) : AndroidViewModel(app) {

    private val _project = MutableStateFlow(Project())
    val project: StateFlow<Project> = _project

    private val _selectedClipId = MutableStateFlow<String?>(null)
    val selectedClipId: StateFlow<String?> = _selectedClipId

    private val _export = MutableStateFlow<ExportState>(ExportState.Idle)
    val export: StateFlow<ExportState> = _export

    /** Cambia cada vez que la lista de clips cambia; el reproductor la usa para recargar. */
    private val _timelineVersion = MutableStateFlow(0)
    val timelineVersion: StateFlow<Int> = _timelineVersion

    fun hasClips(): Boolean = _project.value.clips.isNotEmpty()

    fun addClips(uris: List<Uri>) {
        viewModelScope.launch {
            val newClips = withContext(Dispatchers.IO) {
                uris.mapNotNull { uri ->
                    val d = readDurationMs(uri) ?: return@mapNotNull null
                    Clip(uri = uri, sourceDurationMs = d)
                }
            }
            if (newClips.isEmpty()) return@launch
            _project.update { it.copy(clips = it.clips + newClips) }
            if (_selectedClipId.value == null) _selectedClipId.value = newClips.first().id
            bump()
        }
    }

    fun select(id: String?) { _selectedClipId.value = id }

    fun trimSelected(startMs: Long, endMs: Long) {
        val id = _selectedClipId.value ?: return
        _project.update { p ->
            p.copy(clips = p.clips.map { c ->
                if (c.id != id) c else {
                    val s = startMs.coerceIn(0L, c.sourceDurationMs - 100)
                    val e = endMs.coerceIn(s + 100, c.sourceDurationMs)
                    c.copy(startMs = s, endMs = e)
                }
            })
        }
        bump()
    }

    /** Divide el clip que contiene el instante global [globalMs] en dos. */
    fun splitAt(globalMs: Long) {
        val p = _project.value
        val (index, offset) = p.locate(globalMs) ?: return
        val clip = p.clips[index]
        // No dividir en los bordes
        if (offset < 200 || clip.durationMs - offset < 200) return
        val cut = clip.startMs + offset
        val first = clip.copy(endMs = cut)
        val second = Clip(uri = clip.uri, sourceDurationMs = clip.sourceDurationMs, startMs = cut, endMs = clip.endMs)
        val clips = p.clips.toMutableList()
        clips[index] = first
        clips.add(index + 1, second)
        _project.value = p.copy(clips = clips)
        _selectedClipId.value = second.id
        bump()
    }

    fun deleteSelected() {
        val id = _selectedClipId.value ?: return
        _project.update { p -> p.copy(clips = p.clips.filterNot { it.id == id }) }
        _selectedClipId.value = _project.value.clips.firstOrNull()?.id
        bump()
    }

    fun moveSelected(delta: Int) {
        val id = _selectedClipId.value ?: return
        _project.update { p ->
            val i = p.clips.indexOfFirst { it.id == id }
            val j = i + delta
            if (i < 0 || j < 0 || j >= p.clips.size) return@update p
            val list = p.clips.toMutableList()
            val tmp = list[i]; list[i] = list[j]; list[j] = tmp
            p.copy(clips = list)
        }
        bump()
    }

    fun setFilter(f: Filter) { _project.update { it.copy(filter = f) }; bump() }

    fun setText(t: TextOverlay?) { _project.update { it.copy(text = t?.takeIf { x -> x.text.isNotBlank() }) } }

    fun setMusic(uri: Uri?) {
        if (uri == null) { _project.update { it.copy(music = null) }; bump(); return }
        viewModelScope.launch {
            val name = withContext(Dispatchers.IO) { displayName(uri) }
            _project.update { it.copy(music = MusicTrack(uri = uri, name = name)) }
            bump()
        }
    }

    fun setMusicVolume(v: Float) {
        _project.update { p -> p.copy(music = p.music?.copy(volume = v.coerceIn(0f, 1f))) }
    }

    fun exportProject() {
        if (_export.value is ExportState.Running) return
        val p = _project.value
        if (p.clips.isEmpty()) return
        _export.value = ExportState.Running(0)
        Exporter(getApplication<Application>()).export(
            project = p,
            onProgress = { _export.value = ExportState.Running(it) },
            onDone = { _export.value = ExportState.Done(it) },
            onError = { _export.value = ExportState.Failed(it) },
        )
    }

    fun resetExport() { _export.value = ExportState.Idle }

    private fun bump() { _timelineVersion.update { it + 1 } }

    private fun readDurationMs(uri: Uri): Long? {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(getApplication<Application>(), uri)
            r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        } catch (e: Exception) {
            null
        } finally {
            r.release()
        }
    }

    private fun displayName(uri: Uri): String {
        val cr = getApplication<Application>().contentResolver
        cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) return c.getString(0) ?: "Pista"
        }
        return uri.lastPathSegment ?: "Pista"
    }
}
