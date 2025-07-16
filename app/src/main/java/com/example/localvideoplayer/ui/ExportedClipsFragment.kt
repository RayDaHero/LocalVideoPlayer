package com.example.localvideoplayer.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.localvideoplayer.databinding.FragmentVideoListBinding
import com.example.localvideoplayer.viewmodel.VideoBrowserViewModel

class ExportedClipsFragment : Fragment() {

    private var _binding: FragmentVideoListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VideoBrowserViewModel by activityViewModels()
    private lateinit var videoAdapter: VideoAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentVideoListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
        viewModel.loadExportedVideos()
    }

    override fun onResume() {
        super.onResume()
        // Refresh the list when the user comes back to this tab
        viewModel.loadExportedVideos()
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
        viewModel.exportedVideos.observe(viewLifecycleOwner) { videos ->
            binding.emptyView.text = "No exported clips found."
            binding.emptyView.visibility = if (videos.isEmpty()) View.VISIBLE else View.GONE
            videoAdapter.submitList(videos)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}