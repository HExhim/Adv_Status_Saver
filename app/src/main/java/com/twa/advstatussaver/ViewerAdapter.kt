package com.twa.advstatussaver

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.VideoView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.io.File
import androidx.core.view.isVisible

class ViewerAdapter(
    private val statusPaths: List<String>
) : RecyclerView.Adapter<ViewerAdapter.ViewerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_viewer_status,
            parent,
            false
        )
        return ViewerViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewerViewHolder, position: Int) {
        val path = statusPaths[position]
        holder.bind(path)
    }

    override fun getItemCount(): Int = statusPaths.size

    override fun onViewDetachedFromWindow(holder: ViewerViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.pauseVideo()
    }

    class ViewerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val statusImage: ImageView = itemView.findViewById(R.id.statusImage)
        private val statusVideo: VideoView = itemView.findViewById(R.id.statusVideo)
        private val btnPlayPause: ImageView = itemView.findViewById(R.id.btnPlayPause)

        private val hideControlsRunnable = Runnable {
            if (statusVideo.isPlaying) {
                btnPlayPause.visibility = View.GONE
            }
        }

        fun bind(path: String) {
            val file = File(path)
            val isVideo = path.endsWith(".mp4", ignoreCase = true)

            // Reset state
            btnPlayPause.removeCallbacks(hideControlsRunnable)

            if (isVideo) {
                statusImage.visibility = View.GONE
                statusVideo.visibility = View.VISIBLE
                // Hide Play/Pause button Initially
                btnPlayPause.visibility = View.GONE
                btnPlayPause.setImageResource(R.drawable.ic_pause_circle)

                statusVideo.setVideoURI(Uri.fromFile(file))
                statusVideo.setOnPreparedListener {
                    btnPlayPause.visibility = View.GONE
                     it.seekTo(1)
                }
                statusVideo.setOnCompletionListener {
                    btnPlayPause.visibility = View.VISIBLE
                    btnPlayPause.setImageResource(R.drawable.ic_play_circle)
                    btnPlayPause.removeCallbacks(hideControlsRunnable)
                }
                
                btnPlayPause.setOnClickListener {
                    if (statusVideo.isPlaying) {
                        pauseVideo()
                    } else {
                        playVideo()
                    }
                }

                statusVideo.setOnClickListener {
                    toggleControls()
                }
            } else {
                statusVideo.visibility = View.GONE
                btnPlayPause.visibility = View.GONE
                statusImage.visibility = View.VISIBLE

                Glide.with(itemView.context)
                    .load(file)
                    .into(statusImage)
            }
        }

        private fun toggleControls() {
            if (btnPlayPause.isVisible) {
                if (statusVideo.isPlaying) {
                    btnPlayPause.visibility = View.GONE
                    btnPlayPause.removeCallbacks(hideControlsRunnable)
                }
            } else {
                showControls()
            }
        }

        private fun showControls() {
            btnPlayPause.visibility = View.VISIBLE
            btnPlayPause.removeCallbacks(hideControlsRunnable)
            if (statusVideo.isPlaying) {
                btnPlayPause.setImageResource(R.drawable.ic_pause_circle)
                btnPlayPause.postDelayed(hideControlsRunnable, 2000)
            } else {
                btnPlayPause.setImageResource(R.drawable.ic_play_circle)
            }
        }

        fun playVideo() {
             if (statusVideo.isVisible) {
                 statusVideo.start()
                 showControls()
             }
        }

        fun pauseVideo() {
            if (statusVideo.isVisible && statusVideo.isPlaying) {
                statusVideo.pause()
                btnPlayPause.removeCallbacks(hideControlsRunnable)
                btnPlayPause.visibility = View.VISIBLE
                btnPlayPause.setImageResource(R.drawable.ic_play_circle)
            }
        }
    }
}