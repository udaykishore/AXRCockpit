package com.ukp.axrcockpit.util

import android.content.Context
import androidx.core.content.edit

class CustomButtonsPreferencesManager(context: Context) {
    private val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "CUSTOM_BUTTONS_PREFS"
        private const val KEY_BUTTON_1_LABEL = "button_1_label"
        private const val KEY_BUTTON_1_COMMAND = "button_1_command"
        private const val KEY_BUTTON_2_LABEL = "button_2_label"
        private const val KEY_BUTTON_2_COMMAND = "button_2_command"
        
        const val DEFAULT_BUTTON_1_LABEL = "Custom 1"
        const val DEFAULT_BUTTON_2_LABEL = "Custom 2"
    }

    fun getDefaultLabel(buttonIndex: Int): String {
        return if (buttonIndex == 1) DEFAULT_BUTTON_1_LABEL else DEFAULT_BUTTON_2_LABEL
    }

    fun getButton1Label(): String = sharedPrefs.getString(KEY_BUTTON_1_LABEL, DEFAULT_BUTTON_1_LABEL) ?: DEFAULT_BUTTON_1_LABEL
    fun getButton1Command(): String = sharedPrefs.getString(KEY_BUTTON_1_COMMAND, "") ?: ""

    fun getButton2Label(): String = sharedPrefs.getString(KEY_BUTTON_2_LABEL, DEFAULT_BUTTON_2_LABEL) ?: DEFAULT_BUTTON_2_LABEL
    fun getButton2Command(): String = sharedPrefs.getString(KEY_BUTTON_2_COMMAND, "") ?: ""

    fun saveButtonConfig(buttonIndex: Int, label: String, command: String) {
        val (labelKey, commandKey) = getKeys(buttonIndex)
        sharedPrefs.edit {
            putString(labelKey, label)
            putString(commandKey, command)
        }
    }

    fun resetButtonConfig(buttonIndex: Int) {
        val (labelKey, commandKey) = getKeys(buttonIndex)
        sharedPrefs.edit {
            remove(labelKey)
            remove(commandKey)
        }
    }

    private fun getKeys(buttonIndex: Int): Pair<String, String> {
        return if (buttonIndex == 1) {
            KEY_BUTTON_1_LABEL to KEY_BUTTON_1_COMMAND
        } else {
            KEY_BUTTON_2_LABEL to KEY_BUTTON_2_COMMAND
        }
    }
}
