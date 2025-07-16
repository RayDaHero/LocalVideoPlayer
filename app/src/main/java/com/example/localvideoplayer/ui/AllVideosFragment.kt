package com.example.localvideoplayer.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.localvideoplayer.databinding.FragmentVideoListBinding
import com.example.localvideoplayer.viewmodel.VideoBrowserViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AllVideosFragment : Fragment() {

    private var _binding: FragmentVideoListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VideoBrowserViewModel by activityViewModels()
    private lateinit var videoAdapter: VideoAdapter

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) viewModel.loadAllVideos() else showPermissionDeniedDialog()
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentVideoListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
        checkPermissionsAndLoadVideos()
    }

    private fun setupRecyclerView() {
        videoAdapter = VideoAdapter { videoItem ->
            val intent = Intent(requireContext(), PlayerActivity::class.java).apply { data = videoItem.uri }
            startActivity(intent)
        }
        binding.videosRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = videoAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { binding.progressBar.visibility = if (it) View.VISIBLE else View.GONE }
        viewModel.allVideos.observe(viewLifecycleOwner) { videos ->
            binding.emptyView.visibility = if (videos.isEmpty()) View.VISIBLE else View.GONE
            videoAdapter.submitList(videos)
        }
    }

    private fun checkPermissionsAndLoadVideos() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_VIDEO else Manifest.permission.READ_EXTERNAL_STORAGE
        when {
            ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED -> viewModel.loadAllVideos()
            shouldShowRequestPermissionRationale(permission) -> showPermissionRationaleDialog(permission)
            else -> requestPermissionLauncher.launch(permission)
        }
    }

    private fun showPermissionRationaleDialog(permission: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Permission Required")
            .setMessage("This app needs access to your videos to display them.")
            .setPositiveButton("OK") { _, _ -> requestPermissionLauncher.launch(permission) }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showPermissionDeniedDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Permission Denied")
            .setMessage("Without permission, the app cannot access your videos. Please enable it in app settings.")
            .setPositiveButton("Go to Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", requireActivity().packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Exit") { _, _ -> requireActivity().finish() }
            .setCancelable(false)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
