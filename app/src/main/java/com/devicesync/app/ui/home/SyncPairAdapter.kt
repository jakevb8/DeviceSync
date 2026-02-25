package com.devicesync.app.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.devicesync.app.data.model.DeviceRole
import com.devicesync.app.data.model.SyncPair
import com.devicesync.app.databinding.ItemSyncPairBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SyncPairAdapter(
    private val onPairClick: (SyncPair) -> Unit,
    private val onSyncNow: (SyncPair) -> Unit
) : ListAdapter<SyncPair, SyncPairAdapter.ViewHolder>(DIFF_CALLBACK) {

    inner class ViewHolder(private val binding: ItemSyncPairBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(pair: SyncPair) {
            binding.tvPairName.text = if (pair.role == DeviceRole.PARENT)
                "📂 ${pair.parentFolderPath}" else "📤 ${pair.childFolderPath}"
            binding.tvDeviceName.text = when (pair.role) {
                DeviceRole.PARENT -> "Parent: ${pair.parentDeviceName}"
                DeviceRole.CHILD -> "→ ${pair.parentDeviceName} (${pair.parentFolderPath})"
            }
            binding.tvLastSynced.text = if (pair.lastSyncedAt > 0)
                "Last synced: ${formatTime(pair.lastSyncedAt)}" else "Never synced"
            binding.chipRole.text = pair.role.name
            binding.root.setOnClickListener { onPairClick(pair) }
            binding.btnSyncNow.setOnClickListener { onSyncNow(pair) }
            // Only child devices can trigger sync
            binding.btnSyncNow.visibility = if (pair.role == DeviceRole.CHILD)
                android.view.View.VISIBLE else android.view.View.GONE
        }

        private fun formatTime(millis: Long): String =
            SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(millis))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemSyncPairBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SyncPair>() {
            override fun areItemsTheSame(a: SyncPair, b: SyncPair) = a.id == b.id
            override fun areContentsTheSame(a: SyncPair, b: SyncPair) = a == b
        }
    }
}
