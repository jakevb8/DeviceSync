package me.jakev.devicesync.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import me.jakev.devicesync.R
import me.jakev.devicesync.data.model.DeviceRole
import me.jakev.devicesync.databinding.FragmentHomeBinding
import me.jakev.devicesync.service.SyncForegroundService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var adapter: SyncPairAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = SyncPairAdapter(
            onPairClick = { pair ->
                findNavController().navigate(
                    R.id.action_home_to_syncDetail,
                    bundleOf("pairId" to pair.id)
                )
            },
            onSyncNow = { pair ->
                requireContext().startForegroundService(
                    SyncForegroundService.startIntent(
                        requireContext(), pair.id, pair.childFolderPath, DeviceRole.PARENT
                    )
                )
            }
        )

        binding.rvSyncPairs.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSyncPairs.adapter = adapter

        binding.fabAddPair.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_addSyncPair)
        }

        viewModel.syncPairs.observe(viewLifecycleOwner) { pairs ->
            binding.emptyState.visibility = if (pairs.isEmpty()) View.VISIBLE else View.GONE
            binding.rvSyncPairs.visibility = if (pairs.isEmpty()) View.GONE else View.VISIBLE
            adapter.submitList(pairs)
        }

        viewModel.isOnWifi.observe(viewLifecycleOwner) { onWifi ->
            binding.wifiWarning.visibility = if (onWifi) View.GONE else View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
