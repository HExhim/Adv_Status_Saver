package com.twa.advstatussaver

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.twa.advstatussaver.viewModels.StatusViewModel

class StatusListFragment : Fragment() {

    companion object {
        private const val ARG_TYPE = "type"

        // Map to ViewModel constants
        const val TYPE_ALL = StatusViewModel.TYPE_ALL
        const val TYPE_IMAGE = StatusViewModel.TYPE_IMAGE
        const val TYPE_VIDEO = StatusViewModel.TYPE_VIDEO

        fun newInstance(type: Int): StatusListFragment {
            val fragment = StatusListFragment()
            val args = Bundle()
            args.putInt(ARG_TYPE, type)
            fragment.arguments = args
            return fragment
        }
    }

    // Use activityViewModels to share the ViewModel with MainActivity
    private val viewModel: StatusViewModel by activityViewModels()
    private lateinit var statusAdapter: StatusAdapter
    private var type: Int = TYPE_ALL
    private var emptyStateText: TextView? = null
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        type = arguments?.getInt(ARG_TYPE) ?: TYPE_ALL
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_status_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val actions = activity as? StatusActions ?: throw IllegalStateException("Activity must implement StatusActions")

        statusAdapter = StatusAdapter(actions)
        val recyclerView: RecyclerView = view.findViewById(R.id.statusRecyclerView)
        emptyStateText = view.findViewById(R.id.emptyStateText)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)

        recyclerView.layoutManager = GridLayoutManager(context, 2)
        recyclerView.adapter = statusAdapter

        // Initial state sync
        statusAdapter.isSelectionMode = viewModel.isSelectionMode.value == true
        
        setupSwipeRefresh()
        observeViewModel()
    }
    
    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener {
            // Trigger reload in ViewModel. 
            // Use current "saved" state to determine which list to reload.
            val isSaved = viewModel.isShowingSaved.value == true
            viewModel.loadStatuses(isSaved)
        }
    }

    private fun observeViewModel() {
        // Observer for the specific list based on type
        val listObserver = { statuses: List<StatusModel> ->
            // Stop refreshing animation when data arrives
            swipeRefreshLayout.isRefreshing = false
            
            if (::statusAdapter.isInitialized) {
                statusAdapter.updateData(statuses)
                updateEmptyStateText(viewModel.isShowingSaved.value == true, statuses.size)
            }
        }

        when (type) {
            TYPE_ALL -> viewModel.allStatuses.observe(viewLifecycleOwner, listObserver)
            TYPE_IMAGE -> viewModel.imageStatuses.observe(viewLifecycleOwner, listObserver)
            TYPE_VIDEO -> viewModel.videoStatuses.observe(viewLifecycleOwner, listObserver)
        }

        // Observer for selection mode changes - automatically updates adapter
        viewModel.isSelectionMode.observe(viewLifecycleOwner) { isSelectionMode ->
            if (::statusAdapter.isInitialized) {
                statusAdapter.isSelectionMode = isSelectionMode
                statusAdapter.notifyDataSetChanged()
                
                // Disable swipe refresh during selection mode to avoid conflicts
                swipeRefreshLayout.isEnabled = !isSelectionMode
            }
        }

        // Observer for saved mode changes (to update empty text)
        viewModel.isShowingSaved.observe(viewLifecycleOwner) { isSaved ->
            if (::statusAdapter.isInitialized) {
                updateEmptyStateText(isSaved, statusAdapter.itemCount)
            }
        }
    }

    private fun updateEmptyStateText(isSavedMode: Boolean, statusCount: Int) {
        if (statusCount == 0) {
            emptyStateText?.visibility = View.VISIBLE
            emptyStateText?.text = getString(R.string.empty_state_text)
        } else {
            emptyStateText?.visibility = View.GONE
        }
    }

    fun setSelectionMode(isSelectionMode: Boolean) {
        if (::statusAdapter.isInitialized) {
            statusAdapter.isSelectionMode = isSelectionMode
            statusAdapter.notifyDataSetChanged()
        }
    }
}
