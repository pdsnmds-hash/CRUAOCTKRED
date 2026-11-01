package com.cruaoctkred.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.cruaoctkred.app.domain.repository.SelectionRepository

private val Context.selectionDataStore by preferencesDataStore("selection")

class DataStoreSelectionRepository(private val context: Context) : SelectionRepository {
    override val savedIds: Flow<Set<String>> = context.selectionDataStore.data.map { it[SAVED].orEmpty() }
    override val comparedIds: Flow<List<String>> = context.selectionDataStore.data.map { preferences ->
        preferences[COMPARED].orEmpty().split('|').filter(String::isNotBlank).take(MAX_COMPARE)
    }

    override suspend fun toggleSaved(id: String) {
        context.selectionDataStore.edit { preferences ->
            val current = preferences[SAVED].orEmpty().toMutableSet()
            if (!current.add(id)) current.remove(id)
            preferences[SAVED] = current
        }
    }

    override suspend fun toggleCompared(id: String): Boolean {
        var accepted = true
        context.selectionDataStore.edit { preferences ->
            val current = preferences[COMPARED].orEmpty().split('|').filter(String::isNotBlank).toMutableList()
            if (id in current) current.remove(id)
            else if (current.size < MAX_COMPARE) current += id
            else accepted = false
            preferences[COMPARED] = current.joinToString("|")
        }
        return accepted
    }

    private companion object {
        val SAVED = stringSetPreferencesKey("saved_card_ids")
        val COMPARED = stringPreferencesKey("compared_card_ids")
        const val MAX_COMPARE = 2
    }
}
