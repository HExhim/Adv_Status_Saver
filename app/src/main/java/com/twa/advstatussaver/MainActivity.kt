package com.twa.advstatussaver

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.twa.advstatussaver.viewModels.StatusViewModel
import kotlinx.coroutines.launch

// Define keys for preference consistency, mirroring the Manager
private const val PREFS_NAME = "AppPrefs"
private const val KEY_THEME_MODE = "theme_mode"

class MainActivity : AppCompatActivity(), StatusActions {

    // ViewModel replaces manual data/state management
    private val viewModel: StatusViewModel by viewModels()

    private lateinit var sharedPreferences: SharedPreferences

    // UI Components
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var customDrawerRoot: LinearLayout
    private lateinit var topAppBar: MaterialToolbar
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var fabOpenWhatsapp: FloatingActionButton

    private lateinit var navigationDrawerManager: NavigationDrawerManager

    private var menu: Menu? = null
    private val tabTitles = arrayOf("All", "Images", "Videos")

    private var lastBackPressTime: Long = 0
    private val doubleBackToExitInterval: Long = 2000 // 2 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme before super.onCreate
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val themeMode = sharedPreferences.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(themeMode)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupViews()
        setSupportActionBar(topAppBar)

        // Initialize NavigationDrawerManager with the correct root view
        navigationDrawerManager = NavigationDrawerManager(this, drawerLayout, customDrawerRoot, topAppBar)
        navigationDrawerManager.setup()

        setupViewPagerAndTabs()
        setupObservers()
        setupBackPressHandler()

        // Check auto-delete on app launch
        checkAutoDelete()
    }

    private fun setupViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        customDrawerRoot = findViewById(R.id.customDrawer)
        topAppBar = findViewById(R.id.topAppBar)
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        loadingIndicator = findViewById(R.id.loadingIndicator)
        fabOpenWhatsapp = findViewById(R.id.fabOpenWhatsapp)

        fabOpenWhatsapp.setOnClickListener {
            openWhatsApp()
        }
    }

    private fun setupObservers() {
        viewModel.isShowingSaved.observe(this) { isSaved ->
            // Update Activity Title based on mode
            supportActionBar?.title = if (isSaved) {
                getString(R.string.nav_saved_title)
            } else {
                getString(R.string.app_name)
            }
            // Show/Hide FAB based on mode
            if (isSaved) {
                fabOpenWhatsapp.hide()
            } else {
                fabOpenWhatsapp.show()
            }
        }

        viewModel.isSelectionMode.observe(this) { isSelectionMode ->
            // Invalidate the menu when selection mode changes
            invalidateOptionsMenu()
            // Notify fragments to update their UI
            updateSelectionModeInFragments(isSelectionMode)
            
            // Hide FAB in selection mode
            if (isSelectionMode) {
                fabOpenWhatsapp.hide()
            } else if (viewModel.isShowingSaved.value == false) {
                fabOpenWhatsapp.show()
            }
        }

        viewModel.selectedStatusCount.observe(this) { _ ->
            // Update the visibility of menu items whenever the selection count changes
            updateDownloadButtonVisibility()
        }

        viewModel.allStatuses.observe(this) {
            // Data change always triggers a fragment refresh via LiveData
            // The loading indicator is managed by the StatusListFragment when it receives data
            loadingIndicator.visibility = View.GONE
        }
    }

    // Function previously in MainActivity, now calls ViewModel
    fun checkAutoDelete() {
        lifecycleScope.launch {
            val deletedCount = viewModel.checkAutoDelete()
            if (deletedCount > 0) {
                Toast.makeText(this@MainActivity, getString(R.string.auto_delete_result, deletedCount), Toast.LENGTH_LONG).show()
            }
        }
    }

    // --- Navigation Handlers ---

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_download_selected -> {
                downloadSelectedStatuses()
                true
            }
            R.id.action_delete_selected -> {
                deleteSelectedStatuses()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun openWhatsApp() {
        val packages = mutableListOf<String>()
        val appNameMap = mapOf(
            "com.whatsapp" to "WhatsApp",
            "com.whatsapp.w4b" to "WhatsApp Business",
            "com.gbwhatsapp" to "GBWhatsApp"
        )

        appNameMap.keys.forEach { pkg ->
            if (isPackageInstalled(pkg)) {
                packages.add(pkg)
            }
        }

        if (packages.isEmpty()) {
            Toast.makeText(this, "WhatsApp is not installed", Toast.LENGTH_SHORT).show()
            return
        }

        if (packages.size == 1) {
            launchApp(packages[0])
        } else {
            // Show dialog to choose
            val items = packages.map { appNameMap[it] ?: it }.toTypedArray()
            android.app.AlertDialog.Builder(this)
                .setTitle("Open WhatsApp")
                .setItems(items) { _, which ->
                    launchApp(packages[which])
                }
                .show()
        }
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun launchApp(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "Unable to open app", Toast.LENGTH_SHORT).show()
        }
    }

    fun switchToStatusMode() {
        viewModel.loadStatuses(false)
    }

    fun switchToSavedMode() {
        viewModel.loadStatuses(true)
    }

    fun isShowingSaved() = viewModel.isShowingSaved.value == true

    fun showHowToUseDialog() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_how_to_use, null)
        dialog.setContentView(view)

        view.findViewById<View>(R.id.btnGotIt)?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    fun showPrivacyPolicyDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle(R.string.privacy_policy_title)
            .setMessage(R.string.privacy_policy_text)
            .setPositiveButton(R.string.close_button) { d, _ -> d.dismiss() }
            .show()
    }
    // --- Menu and Fragment Management ---

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        this.menu = menu
        updateDownloadButtonVisibility() // Initial update
        return true
    }

    private fun setupViewPagerAndTabs() {
        viewPager.adapter = CategoryPagerAdapter(this)

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()

        viewPager.isUserInputEnabled = true
    }

    /**
     * Finds and notifies currently visible StatusListFragments about selection mode change.
     */
    private fun updateSelectionModeInFragments(isSelectionMode: Boolean) {
        val adapter = viewPager.adapter as? CategoryPagerAdapter ?: return
        // Iterate through all fragments managed by the adapter
        for (i in 0 until adapter.itemCount) {
            // Find fragment by the tag ViewPager2 uses: "f" + itemId.
            val fragmentTag = "f${adapter.getItemId(i)}"
            val fragment = supportFragmentManager.findFragmentByTag(fragmentTag) as? StatusListFragment
            fragment?.setSelectionMode(isSelectionMode)
        }
    }

    // --- StatusActions Implementation (Communication from Fragments) ---

    override fun onCheckBoxClicked(status: StatusModel) {
        viewModel.toggleStatusSelection(status)
    }

    override fun onStatusLongClicked(status: StatusModel): Boolean {
        if (viewModel.isSelectionMode.value == false) {
            viewModel.enterSelectionMode(status)
            return true
        }
        return false
    }

    override fun onStatusClicked(status: StatusModel) {
        if (viewModel.isSelectionMode.value == true) {
            onCheckBoxClicked(status)
        } else {
            // Determine context list for swiping
            val currentTabType = viewPager.currentItem
            val listToPass = viewModel.getStatusListForViewer(currentTabType)

            val intent = Intent(this, ViewerActivity::class.java).apply {
                putExtra("STATUS_PATH", status.file.absolutePath)

                // Pass all paths of the current filtered list for swipe viewing
                val allPaths = ArrayList(listToPass.map { it.file.absolutePath })
                putStringArrayListExtra("ALL_STATUS_PATHS", allPaths)
                // Determine the index of the clicked status within the current list
                val startIndex = listToPass.indexOfFirst { it.file.absolutePath == status.file.absolutePath }
                putExtra("START_INDEX", startIndex)
            }
            startActivity(intent)
        }
    }

    // --- Menu and Back Press Logic (Updated to use ViewModel state) ---

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    drawerLayout.isDrawerOpen(GravityCompat.START) -> {
                        drawerLayout.closeDrawer(GravityCompat.START)
                    }
                    viewModel.isSelectionMode.value == true -> {
                        viewModel.exitSelectionMode()
                    }
                    viewModel.isShowingSaved.value == true -> {
                        switchToStatusMode() // Back from Saved to Recent
                    }
                    else -> {
                        // Double back to exit
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastBackPressTime < doubleBackToExitInterval) {
                            finish()
                        } else {
                            lastBackPressTime = currentTime
                            Toast.makeText(this@MainActivity, "Press back again to exit", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
    }

    /**
     * Updates the visibility of the download and delete actions in the toolbar menu.
     */
    private fun updateDownloadButtonVisibility() {
        val anySelected = (viewModel.selectedStatusCount.value ?: 0) > 0
        val isSavedMode = viewModel.isShowingSaved.value == true

        menu?.apply {
            if (isSavedMode) {
                // In Saved Mode: Show Delete, Hide Download
                findItem(R.id.action_download_selected)?.isVisible = false
                findItem(R.id.action_delete_selected)?.isVisible = anySelected
            } else {
                // In Status Mode: Show Download, Hide Delete
                findItem(R.id.action_download_selected)?.isVisible = anySelected
                findItem(R.id.action_delete_selected)?.isVisible = false
            }
        }
    }

    /**
     * Downloads selected statuses.
     */
    private fun downloadSelectedStatuses() {
        val selectedCount = viewModel.selectedStatusCount.value ?: 0
        if (selectedCount == 0) return

        loadingIndicator.visibility = View.VISIBLE

        lifecycleScope.launch {
            val successCount = viewModel.saveSelectedStatuses()
            loadingIndicator.visibility = View.GONE

            val message = if (successCount > 0) {
                getString(R.string.status_saved_count, successCount)
            } else {
                "Failed to save"
            }
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Deletes selected statuses (only available in Saved mode).
     */
    private fun deleteSelectedStatuses() {
        val selectedCount = viewModel.selectedStatusCount.value ?: 0
        if (selectedCount == 0) return

        android.app.AlertDialog.Builder(this)
            .setTitle(R.string.delete_statuses_title)
            .setMessage(getString(R.string.delete_statuses_confirm, selectedCount))
            .setPositiveButton(R.string.delete_button) { _, _ ->
                loadingIndicator.visibility = View.VISIBLE

                lifecycleScope.launch {
                    val successCount = viewModel.deleteSelectedStatuses()
                    loadingIndicator.visibility = View.GONE

                    Toast.makeText(this@MainActivity, getString(R.string.status_deleted_count, successCount), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel_button, null)
            .show()
    }
}