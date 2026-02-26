package com.devicesync.app.ui.sync

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.devicesync.app.data.model.DeviceRole
import com.devicesync.app.data.model.SyncPair
import com.devicesync.app.data.repository.SyncPairRepository
import com.devicesync.app.util.DeviceUtil
import com.devicesync.app.util.NetworkUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AddSyncPairUiState {
    object Idle : AddSyncPairUiState()
    object Loading : AddSyncPairUiState()
    object Success : AddSyncPairUiState()
    data class Error(val message: String) : AddSyncPairUiState()
}

@HiltViewModel
class AddSyncPairViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val syncPairRepository: SyncPairRepository,
    private val deviceUtil: DeviceUtil,
    private val networkUtil: NetworkUtil
) : ViewModel() {

    private val uid get() = auth.currentUser?.uid ?: ""

    private val _uiState = MutableLiveData<AddSyncPairUiState>(AddSyncPairUiState.Idle)
    val uiState: LiveData<AddSyncPairUiState> = _uiState

    /** All pairs for this account — shown so user can pick which parent to link to. */
    val availableParentPairs: LiveData<List<SyncPair>> =
        syncPairRepository.observeSyncPairs(uid).asLiveData()

    /**
     * Register this device as a PARENT with the selected folder.
     * Writes to Firestore so any other signed-in device sees it immediately.
     */
    fun registerAsParent(folderUri: String) {
        viewModelScope.launch {
            _uiState.value = AddSyncPairUiState.Loading
            val pair = SyncPair(
                parentDeviceName = deviceUtil.getDeviceName(),
                parentDeviceId = deviceUtil.getDeviceId(),
                parentFolderPath = folderUri,
                role = DeviceRole.PARENT
            )
            val result = syncPairRepository.registerParentPair(uid, pair)
            _uiState.value = if (result.isSuccess) AddSyncPairUiState.Success
            else AddSyncPairUiState.Error(result.exceptionOrNull()?.message ?: "Failed")
        }
    }

    /**
     * Link this device as a CHILD to an existing parent pair.
     * Saves the child's current WiFi IP into Firestore so the parent can connect.
     */
    fun linkAsChild(pairId: String, childFolderUri: String) {
        viewModelScope.launch {
            _uiState.value = AddSyncPairUiState.Loading
            val ip = networkUtil.getWifiIpAddress()
            if (ip.isBlank()) {
                _uiState.value = AddSyncPairUiState.Error("Not connected to WiFi — can't determine IP address")
                return@launch
            }
            val result = syncPairRepository.linkChildToPair(
                uid = uid,
                pairId = pairId,
                childDeviceName = deviceUtil.getDeviceName(),
                childDeviceId = deviceUtil.getDeviceId(),
                childFolderPath = childFolderUri,
                childIpAddress = ip
            )
            _uiState.value = if (result.isSuccess) AddSyncPairUiState.Success
            else AddSyncPairUiState.Error(result.exceptionOrNull()?.message ?: "Failed")
        }
    }
}

sealed class AddSyncPairUiState {
    object Idle : AddSyncPairUiState()
    object Loading : AddSyncPairUiState()
    object Success : AddSyncPairUiState()
    data class Error(val message: String) : AddSyncPairUiState()
}

@HiltViewModel
class AddSyncPairViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val syncPairRepository: SyncPairRepository,
    private val deviceUtil: DeviceUtil
) : ViewModel() {

    private val uid get() = auth.currentUser?.uid ?: ""

    private val _uiState = MutableLiveData<AddSyncPairUiState>(AddSyncPairUiState.Idle)
    val uiState: LiveData<AddSyncPairUiState> = _uiState

    /** Parent pairs registered by this user that don't yet have a child device linked. */
    val availableParentPairs: LiveData<List<SyncPair>> =
        syncPairRepository.observeSyncPairs(uid).asLiveData()

    /**
     * Register this device as a PARENT with the selected folder.
     * The record is written to Firestore so any other logged-in device can discover it.
     */
    fun registerAsParent(folderUri: String) {
        viewModelScope.launch {
            _uiState.value = AddSyncPairUiState.Loading
            val pair = SyncPair(
                parentDeviceName = deviceUtil.getDeviceName(),
                parentDeviceId = deviceUtil.getDeviceId(),
                parentFolderPath = folderUri,
                role = DeviceRole.PARENT
            )
            val result = syncPairRepository.registerParentPair(uid, pair)
            _uiState.value = if (result.isSuccess) AddSyncPairUiState.Success
            else AddSyncPairUiState.Error(result.exceptionOrNull()?.message ?: "Failed")
        }
    }

    /**
     * Link this device as a CHILD to an existing parent pair.
     */
    fun linkAsChild(pairId: String, childFolderUri: String) {
        viewModelScope.launch {
            _uiState.value = AddSyncPairUiState.Loading
            val result = syncPairRepository.linkChildToPair(
                uid = uid,
                pairId = pairId,
                childDeviceName = deviceUtil.getDeviceName(),
                childDeviceId = deviceUtil.getDeviceId(),
                childFolderPath = childFolderUri
            )
            _uiState.value = if (result.isSuccess) AddSyncPairUiState.Success
            else AddSyncPairUiState.Error(result.exceptionOrNull()?.message ?: "Failed")
        }
    }
}
