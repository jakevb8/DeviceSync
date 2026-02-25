package com.devicesync.app.ui.sync

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.devicesync.app.data.model.SyncFile
import com.devicesync.app.data.model.SyncPair
import com.devicesync.app.data.repository.SyncFileRepository
import com.devicesync.app.data.repository.SyncPairRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncDetailViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val syncPairRepository: SyncPairRepository,
    private val syncFileRepository: SyncFileRepository
) : ViewModel() {

    private val uid get() = auth.currentUser?.uid ?: ""

    private val _pair = MutableLiveData<SyncPair?>()
    val pair: LiveData<SyncPair?> = _pair

    private var _files: LiveData<List<SyncFile>> = MutableLiveData(emptyList())
    val files: LiveData<List<SyncFile>> get() = _files

    fun loadPair(pairId: String) {
        // Observe remote files from Firestore for this pair
        _files = syncPairRepository.observeFilesForPair(uid, pairId).asLiveData()

        // Load pair details
        viewModelScope.launch {
            syncPairRepository.observeSyncPairs(uid).collect { pairs ->
                _pair.value = pairs.find { it.id == pairId }
            }
        }
    }
}
