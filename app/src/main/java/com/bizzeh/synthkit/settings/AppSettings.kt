package com.bizzeh.synthkit.settings

import com.bizzeh.synthkit.project.Quantise

enum class ThemeChoice { SYSTEM, LIGHT, DARK }

/** App-wide choices. The click and quantise values only seed new projects and tracks. */
data class AppSettings(
    val theme: ThemeChoice = ThemeChoice.SYSTEM,
    val haptics: Boolean = true,
    val clickInNewProjects: Boolean = false,
    val newTrackQuantise: Quantise = Quantise.OFF,
)

/** What the settings screen can change. */
class SettingsActions(
    val setTheme: (ThemeChoice) -> Unit,
    val setHaptics: (Boolean) -> Unit,
    val setClickInNewProjects: (Boolean) -> Unit,
    val setNewTrackQuantise: (Quantise) -> Unit,
)
