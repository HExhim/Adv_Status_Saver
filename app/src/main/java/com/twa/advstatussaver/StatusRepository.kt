package com.twa.advstatussaver

import android.content.Context
import android.media.MediaScannerConnection
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class StatusRepository(private val context: Context) {

    /**
     * Finds all valid statuses (images and videos) in ALL WhatsApp status directories.
     * Uses Dispatchers.IO for background disk access.
     */
    suspend fun getStatuses(): List<StatusModel> = withContext(Dispatchers.IO) {
        val allStatuses = mutableListOf<StatusModel>()
        
        for (path in AppConstants.STATUS_DIRECTORIES) {
            val statusDir = File(path)
            
            if (statusDir.exists() && statusDir.isDirectory) {
                Log.d("Repo", "Scanning directory: $path")
                val files = statusDir.listFiles()
                
                if (files != null) {
                    val dirStatuses = files.filter { it.isFile && it.name != ".nomedia" }
                        .mapNotNull { file ->
                            val isVideo = file.name.endsWith(".mp4", ignoreCase = true)
                            val isImage = file.name.endsWith(".jpg", ignoreCase = true) || 
                                          file.name.endsWith(".jpeg", ignoreCase = true)

                            if (isVideo || isImage) {
                                // Try to identify source from path
                                val source = when {
                                    path.contains("com.whatsapp.w4b") || path.contains("WhatsApp Business") -> "Business"
                                    path.contains("GBWhatsApp") -> "GB"
                                    else -> "Standard"
                                }
                                StatusModel(file = file, isVideo = isVideo, sourceApp = source)
                            } else {
                                null
                            }
                        }
                    allStatuses.addAll(dirStatuses)
                }
            } else {
               // Log.v("Repo", "Directory not found: $path")
            }
        }

        return@withContext allStatuses.sortedByDescending { it.file.lastModified() }
    }
    
    /**
     * Retrieves all saved statuses from the "ADV Status Saver" directory.
     */
    suspend fun getSavedStatuses(): List<StatusModel> = withContext(Dispatchers.IO) {
        val savedStatuses = mutableListOf<StatusModel>()
        val savedDir = AppConstants.SAVE_DIR
        
        if (savedDir.exists() && savedDir.isDirectory) {
             val files = savedDir.listFiles()
             if (files != null) {
                 val statuses = files.filter { it.isFile && it.name != ".nomedia" }
                     .mapNotNull { file ->
                         val isVideo = file.name.endsWith(".mp4", ignoreCase = true)
                         val isImage = file.name.endsWith(".jpg", ignoreCase = true) || 
                                       file.name.endsWith(".jpeg", ignoreCase = true)
                         
                         if (isVideo || isImage) {
                             StatusModel(file = file, isVideo = isVideo, sourceApp = "Saved")
                         } else {
                             null
                         }
                     }
                 savedStatuses.addAll(statuses)
             }
        }
        
        return@withContext savedStatuses.sortedByDescending { it.file.lastModified() }
    }

    /**
     * Copies a status file to the public storage directory and updates the media store.
     * Uses Dispatchers.IO for background file copy.
     */
    suspend fun saveStatus(status: StatusModel): Boolean = withContext(Dispatchers.IO) {
        val destDir = AppConstants.SAVE_DIR
        if (!destDir.exists()) {
            destDir.mkdirs()
        }

        val sourceFile = status.file
        val destFile = File(destDir, sourceFile.name)

        if (destFile.exists()) {
            // Already saved, prevent redundant copy
            Log.d("Repo", "File already exists: ${destFile.name}")
            return@withContext true
        }

        try {
            FileInputStream(sourceFile).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Crucial step: Notify the MediaStore so the file appears instantly in the gallery
            MediaScannerConnection.scanFile(
                context,
                arrayOf(destFile.absolutePath),
                arrayOf(if (status.isVideo) AppConstants.MIME_TYPE_VIDEO else AppConstants.MIME_TYPE_IMAGE)
            ) { path, uri ->
                Log.d("Repo", "MediaScan completed for $path, URI: $uri")
            }
            return@withContext true
        } catch (e: IOException) {
            Log.e("Repo", "Error saving status: ${e.message}", e)
            return@withContext false
        }
    }
    
    /**
     * Deletes a saved status file and updates the media store.
     */
    suspend fun deleteStatus(status: StatusModel): Boolean = withContext(Dispatchers.IO) {
        val file = status.file
        if (file.exists()) {
            if (file.delete()) {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(file.absolutePath),
                    null
                ) { path, uri ->
                    Log.d("Repo", "MediaScan (delete) completed for $path, URI: $uri")
                }
                return@withContext true
            }
        }
        return@withContext false
    }
}