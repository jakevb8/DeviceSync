package me.jakev.devicesync.ui.sync

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import me.jakev.devicesync.data.model.SyncFile
import me.jakev.devicesync.data.model.SyncPair
import me.jakev.devicesync.data.repository.SyncFileRepository
import me.jakev.devicesync.data.repository.SyncPairRepository
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
        // File sync state is tracked locally in Room — no Firestore file records
        _files = syncFileRepository.getFilesForPair(pairId).asLiveData()

        viewModelScope.launch {
            syncPairRepository.observeSyncPairs(uid).collect { pairs ->
                _pair.value = pairs.find { it.id == pairId }
            }
        }
    }
}
