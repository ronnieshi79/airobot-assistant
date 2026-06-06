package com.airobot.airbot.character

/**
 * Supported character rendering engine types.
 * Parsed from the persisted string in AiRobot.characterType.
 */
enum class CharacterType {
    ANDROID_CANVAS,  // Aether robot (Compose Canvas drawn)
    RIVE_IP;         // Rive-based IP character (e.g. Xin Xiao Miao)

    companion object {
        fun fromString(value: String): CharacterType =
            entries.find { it.name == value } ?: ANDROID_CANVAS
    }
}
