package com.nexo.launcher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexo.launcher.data.AppInfo
import com.nexo.launcher.data.AppRepository
import com.nexo.launcher.data.Folder
import com.nexo.launcher.data.HomeItem
import com.nexo.launcher.data.LauncherSettings
import com.nexo.launcher.data.SettingsStore
import com.nexo.launcher.data.WidgetEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Collections
import java.util.UUID

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    val repo = AppRepository(application)
    private val store = SettingsStore(application)

    val apps: StateFlow<List<AppInfo>> = repo.apps
    val settings: StateFlow<LauncherSettings> =
        store.settings.stateIn(viewModelScope, SharingStarted.Eagerly, LauncherSettings())

    fun update(transform: (LauncherSettings) -> LauncherSettings) {
        viewModelScope.launch { store.update(transform) }
    }

    // --- Pantalla principal ---
    fun toggleHome(item: String) = update { s ->
        if (item in s.homeItems) s.copy(homeItems = s.homeItems - item)
        else s.copy(homeItems = s.homeItems + item)
    }

    fun moveHome(item: String, delta: Int) = update { s -> s.copy(homeItems = s.homeItems.moved(item, delta)) }

    // --- Apps ocultas ---
    fun setHidden(key: String, hidden: Boolean) = update { s ->
        if (hidden) s.copy(hidden = s.hidden + key, homeItems = s.homeItems - HomeItem.app(key))
        else s.copy(hidden = s.hidden - key)
    }

    // --- Carpetas ---
    fun createFolder(name: String, firstApp: String?) = update { s ->
        val folder = Folder(UUID.randomUUID().toString().take(8), name.ifBlank { "Carpeta" }, listOfNotNull(firstApp))
        s.copy(folders = s.folders + folder, homeItems = s.homeItems + HomeItem.folder(folder.id))
    }

    fun addToFolder(folderId: String, key: String) = update { s ->
        s.copy(folders = s.folders.map { if (it.id == folderId && key !in it.apps) it.copy(apps = it.apps + key) else it })
    }

    fun removeFromFolder(folderId: String, key: String) = update { s ->
        s.copy(folders = s.folders.map { if (it.id == folderId) it.copy(apps = it.apps - key) else it })
    }

    fun renameFolder(folderId: String, name: String) = update { s ->
        s.copy(folders = s.folders.map { if (it.id == folderId) it.copy(name = name.ifBlank { it.name }) else it })
    }

    fun deleteFolder(folderId: String) = update { s ->
        s.copy(folders = s.folders.filterNot { it.id == folderId }, homeItems = s.homeItems - HomeItem.folder(folderId))
    }

    // --- Widgets ---
    fun addWidget(id: Int, heightDp: Int) = update { s -> s.copy(widgets = s.widgets + WidgetEntry(id, heightDp)) }

    fun removeWidget(id: Int) = update { s -> s.copy(widgets = s.widgets.filterNot { it.id == id }) }

    fun resizeWidget(id: Int, deltaDp: Int) = update { s ->
        s.copy(widgets = s.widgets.map {
            if (it.id == id) it.copy(heightDp = (it.heightDp + deltaDp).coerceIn(60, 600)) else it
        })
    }

    fun moveWidget(id: Int, delta: Int) = update { s ->
        val entry = s.widgets.firstOrNull { it.id == id }
        if (entry == null) s else s.copy(widgets = s.widgets.moved(entry, delta))
    }
}

fun <T> List<T>.moved(item: T, delta: Int): List<T> {
    val i = indexOf(item)
    val j = i + delta
    if (i < 0 || j !in indices) return this
    return toMutableList().also { Collections.swap(it, i, j) }
}
