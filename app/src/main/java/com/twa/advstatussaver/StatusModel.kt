package com.twa.advstatussaver

import android.os.Environment
import java.io.File

// --- Constants (Critical paths and media types) ---

object AppConstants {
    
    // List of potential Status directories for various WhatsApp versions (Standard, Business, GB, etc.)
    // Covers both legacy paths and Android 11+ "Android/media" paths.
    val STATUS_DIRECTORIES = listOf(
        // Standard WhatsApp
        "${Environment.getExternalStorageDirectory()}/Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
        "${Environment.getExternalStorageDirectory()}/WhatsApp/Media/.Statuses",

        // WhatsApp Business
        "${Environment.getExternalStorageDirectory()}/Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses",
        "${Environment.getExternalStorageDirectory()}/WhatsApp Business/Media/.Statuses",

        // GBWhatsApp
        "${Environment.getExternalStorageDirectory()}/GBWhatsApp/Media/.Statuses"
    )

    const val SAVE_DIR_NAME = "ADV Status Saver"
    val SAVE_DIR = File("${Environment.getExternalStorageDirectory()}/$SAVE_DIR_NAME")

    const val MIME_TYPE_IMAGE = "image/jpeg"
    const val MIME_TYPE_VIDEO = "video/mp4"

    const val PERMISSION_REQUEST_CODE = 101
}

// --- Status Data Class ---

/**
 * Data class representing a single WhatsApp status file.
 * @property file The actual status file object.
 * @property isVideo True if the file is a video, false if it's an image.
 * @property sourceApp The app this status came from (Standard, Business, GB, Saved).
 * @property isSelected Selection state for UI (transient).
 */
data class StatusModel(
    val file: File,
    val isVideo: Boolean,
    val sourceApp: String = "Unknown",
    var isSelected: Boolean = false
)