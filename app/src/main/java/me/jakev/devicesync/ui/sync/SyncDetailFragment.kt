package me.jakev.devicesync.ui.sync

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import me.jakev.devicesync.databinding.FragmentSyncDetailBinding
import me.jakev.devicesync.util.FileUtil
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class SyncDetailFragment : Fragment() {

    private var _binding: FragmentSyncDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SyncDetailViewModel by viewModels()
    private val pairId: String by lazy { requireArguments().getString("pairId")!! }
    private lateinit var adapter: SyncFileAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSyncDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.loadPair(pairId)

        adapter = SyncFileAdapter()
        binding.rvFiles.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFiles.adapter = adapter

        viewModel.pair.observe(viewLifecycleOwner) { pair ->
            pair ?: return@observe
            binding.tvFolderPath.text = pair.childFolderPath
            binding.tvParentDevice.text = "Syncing to: ${pair.parentDeviceName}"
            binding.tvLastSynced.text = if (pair.lastSyncedAt > 0)
                "Last synced: ${formatTime(pair.lastSyncedAt)}" else "Never synced"
        }

        viewModel.files.observe(viewLifecycleOwner) { files ->
            adapter.submitList(files)
            binding.tvFileCount.text = "${files.size} files"
            val synced = files.count { it.syncStatus.name == "SYNCED" }
            binding.tvSyncedCount.text = "$synced / ${files.size} synced"
            binding.syncProgress.max = files.size
            binding.syncProgress.progress = synced
        }
    }

    private fun formatTime(millis: Long): String =
        SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date(millis))

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
