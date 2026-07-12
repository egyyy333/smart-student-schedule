package com.smartstudentschedule.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "smart_student_settings")

class AppRepository(private val context: Context) {
    private val gson = Gson()
    private val stateKey = stringPreferencesKey("app_state_json")

    val appStateFlow: Flow<AppState> = context.dataStore.data.map { preferences ->
        val json = preferences[stateKey]
        if (json != null) {
            try {
                gson.fromJson(json, AppState::class.java)
            } catch (e: Exception) {
                AppState()
            }
        } else {
            AppState()
        }
    }

    suspend fun saveState(state: AppState) {
        context.dataStore.edit { preferences ->
            preferences[stateKey] = gson.toJson(state)
        }
    }
}
