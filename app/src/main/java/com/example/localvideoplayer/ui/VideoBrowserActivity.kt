package com.example.localvideoplayer.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.localvideoplayer.databinding.ActivityVideoBrowserBinding
import com.google.android.material.tabs.TabLayoutMediator

class VideoBrowserActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoBrowserBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
}