package com.glasslauncher.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet

/**
 * Launch counts and recent apps, kept on the device only. Powers Suggestions in search and the App Library,
 * and the recent apps in the iPad Dock — without needing Android's usage-access permission.
 */
class UsageStore(context: Context) {
    private val prefs = context.getSharedPreferences("usage", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(read())
    val usage: StateFlow<Usage> = state.asStateFlow()

    fun recordLaunch(key: String) {
        val next = state.updateAndGet { current ->
            Usage(
                counts = current.counts + (key to (current.counts[key] ?: 0) + 1),
                recents = (listOf(key) + current.recents.filter { it != key }).take(MAX_RECENTS),
            )
        }
        prefs.edit {
            putString(KEY_COUNTS, next.counts.entries.joinToString(ENTRY_SEPARATOR) { "${it.key}$VALUE_SEPARATOR${it.value}" })
            putString(KEY_RECENTS, next.recents.joinToString(ENTRY_SEPARATOR))
        }
    }

    private fun read(): Usage {
        val counts = prefs.getString(KEY_COUNTS, null).orEmpty()
            .split(ENTRY_SEPARATOR)
            .mapNotNull { entry ->
                val key = entry.substringBeforeLast(VALUE_SEPARATOR, "")
                val count = entry.substringAfterLast(VALUE_SEPARATOR, "").toIntOrNull()
                if (key.isEmpty() || count == null) null else key to count
            }
            .toMap()
        val recents = prefs.getString(KEY_RECENTS, null).orEmpty().split(ENTRY_SEPARATOR).filter { it.isNotEmpty() }
        return Usage(counts, recents)
    }

    private companion object {
        const val KEY_COUNTS = "counts"
        const val KEY_RECENTS = "recents"
        const val ENTRY_SEPARATOR = "|"
        const val VALUE_SEPARATOR = "="
        const val MAX_RECENTS = 12
    }
}
