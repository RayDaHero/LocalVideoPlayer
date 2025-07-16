package com.example.localvideoplayer.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import com.example.localvideoplayer.databinding.ActivityVideoBrowserBinding
import com.example.localvideoplayer.viewmodel.VideoBrowserViewModel
import com.google.android.material.tabs.TabLayoutMediator

class VideoBrowserActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoBrowserBinding
    private val viewModel: VideoBrowserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityVideoBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.post {
            setupViewPager()
            setupSearchView()
        }
    }

    private fun setupViewPager() {
        val viewPager = binding.viewPager
        val tabLayout = binding.tabLayout

        val adapter = ViewPagerAdapter(this)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "All Videos"
                1 -> "Exported Clips"
                else -> null
            }
        }.attach()
    }

    // --- ADDED: Setup listener for the SearchView ---
    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Perform search when user submits
                viewModel.loadAllVideos(query)
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Perform search as user types
                viewModel.loadAllVideos(newText)
                return false
            }
        })
    }
}