package com.twa.advstatussaver

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.appbar.MaterialToolbar
import android.widget.LinearLayout

// Define constants for SharedPreferences keys
private const val PREFS_NAME = "AppPrefs"
private const val KEY_AUTO_DELETE = "auto_delete_days"
private const val KEY_THEME_MODE = "theme_mode"

class NavigationDrawerManager(
    // We update Toolbar to MaterialToolbar as used in the layout
    private val activity: MainActivity,
    private val drawerLayout: DrawerLayout,
    private val customDrawerLayout: LinearLayout, // This is the root of drawer_layout.xml
    private val toolbar: MaterialToolbar
) {

    private lateinit var toggle: ActionBarDrawerToggle
    private val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Sets up the ActionBarDrawerToggle for the drawer and initializes view listeners.
     */
    fun setup() {
        toggle = ActionBarDrawerToggle(
            activity, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        setupViews()
    }

    private fun setupViews() {
        // --- Navigation Items ---

        // 1. Recent Statuses (Tab 0)
        customDrawerLayout.findViewById<View>(R.id.nav_statuses)?.setOnClickListener {
            // Assuming activity has this method to switch ViewPager to Statuses tab (index 0)
            activity.switchToStatusMode()
            closeDrawer()
        }

        // 2. Saved Statuses (Tab 1)
        customDrawerLayout.findViewById<View>(R.id.nav_saved)?.setOnClickListener {
            // Assuming activity has this method to switch ViewPager to Saved tab (index 1)
            activity.switchToSavedMode()
            closeDrawer()
        }

        // --- Settings/Options ---

        // 3. Auto Delete Spinner
        setupAutoDeleteSpinner()

        // 4. Theme Spinner
        setupThemeSpinner()

        // --- About Items (Now direct links) ---

        // 5. How to Use
        customDrawerLayout.findViewById<View>(R.id.nav_how_to_use)?.setOnClickListener {
            activity.showHowToUseDialog()
            closeDrawer()
        }

        // 6. Privacy Policy
        customDrawerLayout.findViewById<View>(R.id.nav_privacy_policy)?.setOnClickListener {
            activity.showPrivacyPolicyDialog()
            closeDrawer()
        }

        // 7. Share App
        customDrawerLayout.findViewById<View>(R.id.nav_share)?.setOnClickListener {
            shareApp()
            closeDrawer()
        }

        // Removed: Obsolete expandable 'About' section logic which is not present in the refactored XML.
    }

    /**
     * Initializes and handles logic for the Auto Delete feature spinner.
     */
    private fun setupAutoDeleteSpinner() {
        val spinnerAutoDelete = customDrawerLayout.findViewById<Spinner>(R.id.spinnerAutoDelete)
        spinnerAutoDelete?.let { spinner ->
            val savedDays = prefs.getInt(KEY_AUTO_DELETE, 0)

            // Map saved value (0, 7, 30, 90) to array index (0, 1, 2, 3)
            val autoDeleteIndex = when(savedDays) {
                7 -> 1
                30 -> 2
                90 -> 3
                else -> 0 // Default is "Never" (0 days)
            }
            spinner.setSelection(autoDeleteIndex)

            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                // Flag to prevent selection being triggered on initial setup
                private var isInitialized = false

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (!isInitialized) {
                        isInitialized = true
                        return
                    }

                    val days = when(position) {
                        1 -> 7
                        2 -> 30
                        3 -> 90
                        else -> 0 // Position 0
                    }

                    if (days != prefs.getInt(KEY_AUTO_DELETE, -1)) {
                        setAutoDelete(days)
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    /**
     * Initializes and handles logic for the Theme selection spinner.
     */
    private fun setupThemeSpinner() {
        val spinnerTheme = customDrawerLayout.findViewById<Spinner>(R.id.spinnerTheme)
        spinnerTheme?.let { spinner ->
            val savedTheme = prefs.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

            // Map saved mode to array index (0=System, 1=Light, 2=Dark)
            val themeIndex = when(savedTheme) {
                AppCompatDelegate.MODE_NIGHT_NO -> 1
                AppCompatDelegate.MODE_NIGHT_YES -> 2
                else -> 0 // Default is MODE_NIGHT_FOLLOW_SYSTEM
            }
            spinner.setSelection(themeIndex)

            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                // Flag to prevent selection being triggered on initial setup
                private var isInitialized = false

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (!isInitialized) {
                        isInitialized = true
                        return
                    }

                    val mode = when(position) {
                        1 -> AppCompatDelegate.MODE_NIGHT_NO
                        2 -> AppCompatDelegate.MODE_NIGHT_YES
                        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM // Position 0
                    }

                    if (mode != prefs.getInt(KEY_THEME_MODE, -999)) {
                        setTheme(mode)
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    /**
     * Saves the auto-delete preference and triggers necessary checks in MainActivity.
     */
    private fun setAutoDelete(days: Int) {
        prefs.edit().putInt(KEY_AUTO_DELETE, days).apply()
        val msg = if (days == 0) "Auto delete disabled" else activity.getString(R.string.setting_auto_delete, days)
        Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
        // Assuming activity.checkAutoDelete() exists and handles deletion logic
        if (days > 0) activity.checkAutoDelete()
    }

    /**
     * Saves the theme preference and applies the new theme mode globally.
     */
    private fun setTheme(mode: Int) {
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply()
        AppCompatDelegate.setDefaultNightMode(mode)
        closeDrawer()
    }

    private fun shareApp() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, "Check out this Status Saver app: [App Store Link Placeholder]")
            type = "text/plain"
        }
        activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.about_share_app)))
    }

    fun closeDrawer() {
        drawerLayout.closeDrawer(GravityCompat.START)
    }

    // NOTE: For the app to compile, you must ensure your strings.xml contains:
    // <string name="auto_delete_set">Auto delete set to %d days</string>
    // <string name="share_app_title">Share App via...</string>
}