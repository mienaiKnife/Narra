package com.mienaiknife.narra.ui.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class ThemeManager(private val context: Context, private val scope: CoroutineScope) {
    private val darkModeKey = booleanPreferencesKey("dark_mode")
    private val dynamicColorKey = booleanPreferencesKey("dynamic_color")
    private val useSystemThemeKey = booleanPreferencesKey("use_system_theme")

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _isDynamicColor = MutableStateFlow(true)
    val isDynamicColor: StateFlow<Boolean> = _isDynamicColor.asStateFlow()

    private val _useSystemTheme = MutableStateFlow(true)
    val useSystemTheme: StateFlow<Boolean> = _useSystemTheme.asStateFlow()

    init {
        scope.launch {
            val prefs = context.dataStore.data.first()
            _isDarkMode.value = prefs[darkModeKey] ?: true
            _isDynamicColor.value = prefs[dynamicColorKey] ?: true
            _useSystemTheme.value = prefs[useSystemThemeKey] ?: true
        }
    }

    fun setDarkMode(enabled: Boolean) {
        scope.launch {
            context.dataStore.edit { prefs ->
                prefs[darkModeKey] = enabled
            }
            _isDarkMode.value = enabled
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        scope.launch {
            context.dataStore.edit { prefs ->
                prefs[dynamicColorKey] = enabled
            }
            _isDynamicColor.value = enabled
        }
    }

    fun setUseSystemTheme(enabled: Boolean) {
        scope.launch {
            context.dataStore.edit { prefs ->
                prefs[useSystemThemeKey] = enabled
            }
            _useSystemTheme.value = enabled
        }
    }
}

open class ThemeViewModel : ViewModel() {
    private var themeManager: ThemeManager? = null
    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _isDynamicColor = MutableStateFlow(true)
    val isDynamicColor: StateFlow<Boolean> = _isDynamicColor.asStateFlow()

    private val _useSystemTheme = MutableStateFlow(true)
    val useSystemTheme: StateFlow<Boolean> = _useSystemTheme.asStateFlow()

    open fun initialize(context: Context) {
        if (themeManager == null) {
            themeManager = ThemeManager(context, viewModelScope)
            viewModelScope.launch {
                themeManager?.isDarkMode?.collect { isDark ->
                    _isDarkMode.value = isDark
                }
            }
            viewModelScope.launch {
                themeManager?.isDynamicColor?.collect { isDynamic ->
                    _isDynamicColor.value = isDynamic
                }
            }
            viewModelScope.launch {
                themeManager?.useSystemTheme?.collect { useSystem ->
                    _useSystemTheme.value = useSystem
                }
            }
        }
    }

    fun setDarkMode(enabled: Boolean) {
        themeManager?.setDarkMode(enabled)
    }

    fun setDynamicColor(enabled: Boolean) {
        themeManager?.setDynamicColor(enabled)
    }

    fun setUseSystemTheme(enabled: Boolean) {
        themeManager?.setUseSystemTheme(enabled)
    }
}