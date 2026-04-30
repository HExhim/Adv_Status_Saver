package com.twa.advstatussaver.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.twa.advstatussaver.StatusModel
import com.twa.advstatussaver.StatusRepository
import kotlinx.coroutines.launch

// Define keys for preference consistency
private const val PREFS_NAME = "AppPrefs"
private const val KEY_AUTO_DELETE = "auto_delete_days"

class StatusViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val TYPE_ALL = 0
        const val TYPE_IMAGE = 1
        const val TYPE_VIDEO = 2
    }

    private val repository = StatusRepository(application)
    private val prefs = application.getSharedPreferences(PREFS_NAME, Application.MODE_PRIVATE)

    // LiveData for the main status list
    private val _allStatuses = MutableLiveData<List<StatusModel>>(emptyList())
    val allStatuses: LiveData<List<StatusModel>> = _allStatuses

    // LiveData for filtered lists
    val imageStatuses: LiveData<List<StatusModel>> = _allStatuses.map { statuses ->
        statuses.filter { !it.isVideo }
    }

    val videoStatuses: LiveData<List<StatusModel>> = _allStatuses.map { statuses ->
        statuses.filter { it.isVideo }
    }

    // UI State Management
    private val _isShowingSaved = MutableLiveData(false)
    val isShowingSaved: LiveData<Boolean> = _isShowingSaved

    private val _isSelectionMode = MutableLiveData(false)
    val isSelectionMode: LiveData<Boolean> = _isSelectionMode

    val selectedStatusCount: LiveData<Int> = _allStatuses.map { statuses ->
        statuses.count { it.isSelected }
    }

    // Initial load
    init {
        loadStatuses(false)
    }

    // Status management functions
    fun loadStatuses(showSaved: Boolean) {
        _isShowingSaved.value = showSaved
        viewModelScope.launch {
            val statuses = if (showSaved) repository.getSavedStatuses() else repository.getStatuses()
            _allStatuses.value = statuses.map { it.copy(isSelected = false) } // Reset selection state on reload
            exitSelectionMode()
        }
    }

    fun toggleStatusSelection(status: StatusModel) {
        val currentList = _allStatuses.value.orEmpty().toMutableList()
        val index = currentList.indexOfFirst { it.file.absolutePath == status.file.absolutePath }

        if (index != -1) {
            val newStatus = status.copy(isSelected = !status.isSelected)
            currentList[index] = newStatus
            _allStatuses.value = currentList

            // Check if we should enter or exit selection mode
            val anySelected = currentList.any { it.isSelected }
            if (_isSelectionMode.value != true && newStatus.isSelected) {
                _isSelectionMode.value = true
            } else if (_isSelectionMode.value == true && !anySelected) {
                exitSelectionMode()
            }
        }
    }

    fun enterSelectionMode(status: StatusModel) {
        if (_isSelectionMode.value != true) {
            _isSelectionMode.value = true
            toggleStatusSelection(status) // Select the status that initiated the mode
        }
    }

    fun exitSelectionMode() {
        if (_isSelectionMode.value == true) {
            _isSelectionMode.value = false
            // Clear all selections
            val currentList = _allStatuses.value.orEmpty()
            if (currentList.any { it.isSelected }) {
                _allStatuses.value = currentList.map { it.copy(isSelected = false) }
            }
        }
    }

    // Action functions

    fun getSelectedStatuses(): List<StatusModel> = _allStatuses.value.orEmpty().filter { it.isSelected }

    suspend fun saveSelectedStatuses(): Int {
        val selected = getSelectedStatuses()
        var successCount = 0
        selected.forEach { if (repository.saveStatus(it)) successCount++ }
        loadStatuses(false) // Refresh recent list after saving
        return successCount
    }

    suspend fun deleteSelectedStatuses(): Int {
        val selected = getSelectedStatuses()
        var successCount = 0
        selected.forEach { if (repository.deleteStatus(it)) successCount++ }
        loadStatuses(true) // Refresh saved list after deletion
        return successCount
    }

    suspend fun checkAutoDelete(): Int {
        val days = prefs.getInt(KEY_AUTO_DELETE, 0)
        if (days <= 0) return 0

        val cutoffTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        val saved = repository.getSavedStatuses()
        var deletedCount = 0

        saved.forEach { status ->
            if (status.file.lastModified() < cutoffTime) {
                if (repository.deleteStatus(status)) {
                    deletedCount++
                }
            }
        }

        // Only refresh the current list if we were showing saved statuses and something was deleted
        if (_isShowingSaved.value == true && deletedCount > 0) {
            loadStatuses(true)
        }

        return deletedCount
    }

    fun getStatusListForViewer(type: Int): List<StatusModel> {
        return when (type) {
            TYPE_ALL -> _allStatuses.value.orEmpty()
            TYPE_IMAGE -> imageStatuses.value.orEmpty()
            TYPE_VIDEO -> videoStatuses.value.orEmpty()
            else -> _allStatuses.value.orEmpty()
        }
    }
}
