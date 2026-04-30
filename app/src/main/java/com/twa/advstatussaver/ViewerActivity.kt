package com.twa.advstatussaver

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import java.io.File

class ViewerActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var btnDownload: FloatingActionButton
    private lateinit var tvTitle: TextView
    private lateinit var viewPager: ViewPager2

    private var allStatusPaths: ArrayList<String>? = null
    private var currentIndex: Int = 0

    private val statusRepository by lazy { StatusRepository(this) }
    private var viewerAdapter: ViewerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_viewer)

        initViews()
        parseIntent()
        setupViewPager()
        setupListeners()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        btnDownload = findViewById(R.id.btnDownload)
        tvTitle = findViewById(R.id.tvTitle)
        viewPager = findViewById(R.id.viewPager)
    }

    private fun parseIntent() {
        val currentStatusPath = intent.getStringExtra("STATUS_PATH")
        allStatusPaths = intent.getStringArrayListExtra("ALL_STATUS_PATHS")

        if (currentStatusPath != null && allStatusPaths != null) {
            currentIndex = allStatusPaths!!.indexOf(currentStatusPath)
        }
    }

    private fun setupViewPager() {
        if (allStatusPaths != null) {
            viewerAdapter = ViewerAdapter(allStatusPaths!!)
            viewPager.adapter = viewerAdapter
            
            // Initial setup
            viewPager.setCurrentItem(currentIndex, false)
            updateTitle(currentIndex)

            viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    currentIndex = position
                    updateTitle(position)
                    manageVideoPlayback(position)
                }
            })
            
            // Try to play initial video after layout
            viewPager.post {
                manageVideoPlayback(currentIndex)
            }
        }
    }
    
    private fun manageVideoPlayback(activePosition: Int) {
        // The ViewPager2 internal RecyclerView
        val recyclerView = viewPager.getChildAt(0) as? RecyclerView ?: return
        
        // Iterate over all attached view holders
        for (i in 0 until recyclerView.childCount) {
            val child = recyclerView.getChildAt(i)
            val holder = recyclerView.getChildViewHolder(child)
            
            if (holder is ViewerAdapter.ViewerViewHolder) {
                if (holder.bindingAdapterPosition == activePosition) {
                    holder.playVideo()
                } else {
                    holder.pauseVideo()
                }
            }
        }
    }

    private fun updateTitle(position: Int) {
         if (allStatusPaths != null && position in allStatusPaths!!.indices) {
             val path = allStatusPaths!![position]
             val isVideo = path.endsWith(".mp4", ignoreCase = true)
             tvTitle.text = if (isVideo) "Video Status" else "Image Status"
         }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnDownload.setOnClickListener {
            if (allStatusPaths != null && currentIndex in allStatusPaths!!.indices) {
                downloadCurrentStatus()
            }
        }
    }

    private fun downloadCurrentStatus() {
        val path = allStatusPaths!![currentIndex]
        val file = File(path)
        val currentIsVideo = path.endsWith(".mp4", ignoreCase = true)

        val statusModel = StatusModel(file, currentIsVideo)

        lifecycleScope.launch {
            val success = statusRepository.saveStatus(statusModel)
            if (success) {
                Toast.makeText(this@ViewerActivity, "Status Saved Successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@ViewerActivity, "Failed to Save", Toast.LENGTH_SHORT).show()
            }
        }
    }
}