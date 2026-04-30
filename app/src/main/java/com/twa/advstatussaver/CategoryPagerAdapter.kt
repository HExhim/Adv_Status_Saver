package com.twa.advstatussaver

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class CategoryPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 3 // All, Images, Videos

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> StatusListFragment.newInstance(StatusListFragment.TYPE_ALL)    // All
            1 -> StatusListFragment.newInstance(StatusListFragment.TYPE_IMAGE)  // Images
            2 -> StatusListFragment.newInstance(StatusListFragment.TYPE_VIDEO)  // Videos
            else -> throw IllegalStateException("Invalid adapter position")
        }
    }
}