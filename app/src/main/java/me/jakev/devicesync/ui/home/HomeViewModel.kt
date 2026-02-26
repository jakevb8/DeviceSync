package me.jakev.devicesync.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import me.jakev.devicesync.data.model.SyncPair
import me.jakev.devicesync.data.repository.SyncPairRepository
import me.jakev.devicesync.util.NetworkUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val syncPairRepository: SyncPairRepository,
    private val networkUtil: NetworkUtil
) : ViewModel() {

    private val uid get() = auth.currentUser?.uid ?: ""

    val syncPairs: LiveData<List<SyncPair>> =
        syncPairRepository.observeSyncPairs(uid).asLiveData()

    /** Poll WiFi status every 5 seconds. */
    val isOnWifi: LiveData<Boolean> = flow {
        while (true) {
            emit(networkUtil.isOnWifi())
            delay(5_000)
        }
    }.asLiveData()

    fun deletePair(pairId: String) {
        viewModelScope.launch {
            syncPairRepository.deletePair(uid, pairId)
        }
    }
}
