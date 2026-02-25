package com.devicesync.app.ui.sync

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.devicesync.app.R
import com.devicesync.app.data.model.SyncFile
import com.devicesync.app.data.model.SyncStatus
import com.devicesync.app.databinding.ItemSyncFileBinding
import com.devicesync.app.util.FileUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SyncFileAdapter : ListAdapter<SyncFile, SyncFileAdapter.ViewHolder>(DIFF_CALLBACK) {

    inner class ViewHolder(private val binding: ItemSyncFileBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(file: SyncFile) {
            binding.tvFileName.text = file.fileName
            binding.tvFileSize.text = FileUtil.formatFileSize(file.fileSizeBytes)
            binding.tvSyncStatus.text = statusLabel(file.syncStatus)
            binding.tvSyncStatus.setTextColor(
                ContextCompat.getColor(binding.root.context, statusColor(file.syncStatus))
            )
            binding.tvSyncedAt.text = if (file.syncedAt > 0)
                "Synced: ${formatTime(file.syncedAt)}" else ""
            binding.tvError.text = file.errorMessage ?: ""
            binding.tvError.visibility = if (file.errorMessage != null)
                android.view.View.VISIBLE else android.view.View.GONE
        }

        private fun statusLabel(status: SyncStatus) = when (status) {
            SyncStatus.PENDING -> "⏳ Pending"
            SyncStatus.SYNCING -> "🔄 Syncing…"
            SyncStatus.SYNCED -> "✅ Synced"
            SyncStatus.MODIFIED -> "✏️ Modified"
            SyncStatus.FAILED -> "❌ Failed"
            SyncStatus.DELETED -> "🗑️ Deleted"
        }

        private fun statusColor(status: SyncStatus) = when (status) {
            SyncStatus.SYNCED -> R.color.status_synced
            SyncStatus.SYNCING -> R.color.status_syncing
            SyncStatus.FAILED -> R.color.status_failed
            SyncStatus.MODIFIED -> R.color.status_modified
            else -> R.color.status_pending
        }

        private fun formatTime(millis: Long): String =
            SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(millis))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemSyncFileBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SyncFile>() {
            override fun areItemsTheSame(a: SyncFile, b: SyncFile) = a.id == b.id
            override fun areContentsTheSame(a: SyncFile, b: SyncFile) = a == b
        }
    }
}
