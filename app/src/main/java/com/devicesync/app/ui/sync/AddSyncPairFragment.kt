package com.devicesync.app.ui.sync

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.devicesync.app.databinding.FragmentAddSyncPairBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddSyncPairFragment : Fragment() {

    private var _binding: FragmentAddSyncPairBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AddSyncPairViewModel by viewModels()
    private var selectedFolderPath: String? = null

    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            requireContext().contentResolver.takePersistableUriPermission(
                it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                        or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            selectedFolderPath = it.toString()
            binding.tvSelectedFolder.text = it.lastPathSegment ?: it.toString()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddSyncPairBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnPickFolder.setOnClickListener { folderPickerLauncher.launch(null) }

        binding.btnRegisterParent.setOnClickListener {
            val folderPath = selectedFolderPath
            if (folderPath == null) {
                Toast.makeText(requireContext(), "Please select a folder", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.registerAsParent(folderPath)
        }

        viewModel.availableParentPairs.observe(viewLifecycleOwner) { pairs ->
            if (pairs.isNotEmpty()) {
                binding.layoutLinkChild.visibility = View.VISIBLE
                val adapter = android.widget.ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    pairs.map { "${it.parentDeviceName} – ${it.parentFolderPath}" }
                )
                binding.spinnerParentPairs.adapter = adapter
            } else {
                binding.layoutLinkChild.visibility = View.GONE
            }
        }

        binding.btnLinkChild.setOnClickListener {
            val folderPath = selectedFolderPath
            val pairs = viewModel.availableParentPairs.value ?: emptyList()
            val selected = binding.spinnerParentPairs.selectedItemPosition
            if (folderPath == null || pairs.isEmpty() || selected < 0) {
                Toast.makeText(requireContext(), "Select a folder and a parent pair", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.linkAsChild(pairs[selected].id, folderPath)
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AddSyncPairUiState.Loading -> binding.progressBar.visibility = View.VISIBLE
                is AddSyncPairUiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Sync pair created!", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
                is AddSyncPairUiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
                else -> binding.progressBar.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
