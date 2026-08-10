package com.bracketzone.touchassistant.core

enum class AssistantAction {

    NONE,

    // Main menu
    MAIN,

    // Navigation
    HOME,
    BACK,
    RECENTS,

    // System panels
    NOTIFICATIONS,
    QUICK_SETTINGS,
    POWER,

    // Window management
    SPLIT_SCREEN,

    // Security / capture
    LOCK,
    SCREENSHOT,

    // Audio
    VOLUME_UP,
    VOLUME_DOWN,
    MUTE_UNMUTE,
    TOGGLE_RINGER,

    // Hardware
    FLASHLIGHT,

    // Display
    AUTO_ROTATE,
    MAX_BRIGHTNESS,

    // Synchronization
    TOGGLE_SYNC,

    // Connectivity
    WI_FI,
    BLUETOOTH,
    LOCATION,
    AIRPLANE_MODE,
    MOBILE_DATA
}