package com.twa.advstatussaver

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.twa.advstatussaver.R

/**
 * Note: R class imports are implicitly handled in a real project structure.
 * Ensure you have R.layout.item_status, R.id.statusThumbnail, etc., defined
 * in your actual project's R class.
 */
interface StatusActions {
    fun onCheckBoxClicked(status: StatusModel)
    fun onStatusClicked(status: StatusModel)
    fun onStatusLongClicked(status: StatusModel): Boolean
}

class StatusAdapter(private val actions: StatusActions) :
    RecyclerView.Adapter<StatusAdapter.StatusViewHolder>() {

    private var statuses: List<StatusModel> = emptyList()
    var isSelectionMode: Boolean = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    fun updateData(newStatuses: List<StatusModel>) {
        this.statuses = newStatuses
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatusViewHolder {
        // Inflate the new R.layout.item_status
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_status,
            parent,
            false
        )
        return StatusViewHolder(view)
    }

    override fun onBindViewHolder(holder: StatusViewHolder, position: Int) {
        val status = statuses[position]
        holder.bind(status, actions, isSelectionMode)
    }

    override fun getItemCount(): Int = statuses.size

    class StatusViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        // --- View Declarations using IDs from item_status.xml ---
        private val thumbnail: ImageView = itemView.findViewById(R.id.statusThumbnail)
        private val checkBoxSelection: CheckBox = itemView.findViewById(R.id.chkSelection)
        private val playIndicator: ImageView = itemView.findViewById(R.id.playIndicator)

        fun bind(status: StatusModel, actions: StatusActions, isSelectionMode: Boolean) {

            // 1. Load Thumbnail using Glide
            Glide.with(itemView.context)
                .load(status.file)
                .apply(RequestOptions.placeholderOf(android.R.drawable.ic_menu_gallery))
                .centerCrop()
                .into(thumbnail)

            // 2. Setup Visibility for Play Icon
            playIndicator.visibility = if (status.isVideo) View.VISIBLE else View.GONE

            // 3. Bind Selection State
            checkBoxSelection.setOnCheckedChangeListener(null) // Remove previous listener to avoid triggers during bind
            checkBoxSelection.isChecked = status.isSelected
            
            // Checkbox visibility based on selection mode
            checkBoxSelection.visibility = if (isSelectionMode) View.VISIBLE else View.GONE

            // 4. Set up listeners
            itemView.setOnClickListener {
                if (isSelectionMode) {
                    actions.onCheckBoxClicked(status)
                } else {
                    actions.onStatusClicked(status)
                }
            }
            
            itemView.setOnLongClickListener {
                actions.onStatusLongClicked(status)
            }

            checkBoxSelection.setOnClickListener {
                // Update model immediately for UI consistency if needed, 
                // but the activity logic is primary.
                // status.isSelected = checkBoxSelection.isChecked 
                actions.onCheckBoxClicked(status)
            }
        }
    }
}