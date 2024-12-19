package com.abhi.vpn.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBar
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import com.abhi.vpn.R
import com.abhi.vpn.fragment.TunnelDetailFragment
import com.abhi.vpn.fragment.TunnelEditorFragment
import com.abhi.vpn.model.ObservableTunnel


class MainActivity : BaseActivity(), FragmentManager.OnBackStackChangedListener {
    private var actionBar: ActionBar? = null
    private var isTwoPaneLayout = false
    private var backPressedCallback: OnBackPressedCallback? = null
    companion object {
        private const val TAG = "MainActivity"
    }
    private fun handleBackPressed() {
        Log.d(TAG, "Handling back press")
        val backStackEntries = supportFragmentManager.backStackEntryCount
        // If the two-pane layout does not have an editor open, going back should exit the app.
        if (isTwoPaneLayout && backStackEntries <= 1) {
            finish()
            return
        }

        if (backStackEntries >= 1)
            supportFragmentManager.popBackStack()

        // Deselect the current tunnel on navigating back from the detail pane to the one-pane list.
        if (backStackEntries == 1)
            selectedTunnel = null
    }

    override fun onBackStackChanged() {
        val backStackEntries = supportFragmentManager.backStackEntryCount
        Log.d(TAG, "Back stack changed: $backStackEntries entries")

        backPressedCallback?.isEnabled = backStackEntries >= 1
        if (actionBar == null) return
        // Do not show the home menu when the two-pane layout is at the detail view (see above).
        val minBackStackEntries = if (isTwoPaneLayout) 2 else 1
        actionBar!!.setDisplayHomeAsUpEnabled(backStackEntries >= minBackStackEntries)
    }
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called")
        setContentView(R.layout.main_activity)
        actionBar = supportActionBar
        isTwoPaneLayout = findViewById<View?>(R.id.master_detail_wrapper) != null
        supportFragmentManager.addOnBackStackChangedListener(this)
        backPressedCallback = onBackPressedDispatcher.addCallback(this) { handleBackPressed() }
        onBackStackChanged()
        createAndStartVPN()
//        findViewById<Button>(R.id.create_hardcoded_tunnel_button)?.setOnClickListener {
//            // Call the hardcoded tunnel creation logic
//            Log.d("MainActivity", "Create VPN Tunnel button clicked")
//            TunnelUtlis.createHardcodedTunnel()
//        }
    }
    private fun createAndStartVPN() {
        // Fetch VPN details and automatically create the tunnel
        TunnelUtlis.createManagedTunnel(this)
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_activity, menu)
        Log.d(TAG, "Creating options menu")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // The back arrow in the action bar should act the same as the back button.
                onBackPressedDispatcher.onBackPressed()
                true
            }

            R.id.menu_action_edit -> {
                supportFragmentManager.commit {
                    replace(R.id.detail_container, TunnelEditorFragment())
                    setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    addToBackStack(null)
                }
                true
            }
            // This menu item is handled by the editor fragment.
            R.id.menu_action_save -> false
            R.id.menu_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSelectedTunnelChanged(
        oldTunnel: ObservableTunnel?,
        newTunnel: ObservableTunnel?
    ): Boolean {
        val fragmentManager = supportFragmentManager
        if (fragmentManager.isStateSaved) {
            return false
        }
        Log.d(TAG, "Selected tunnel changed from $oldTunnel to $newTunnel")
        val backStackEntries = fragmentManager.backStackEntryCount
        if (newTunnel == null) {
            // Clear everything off the back stack (all editors and detail fragments).
            fragmentManager.popBackStackImmediate(0, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            return true
        }
        if (backStackEntries == 2) {
            // Pop the editor off the back stack to reveal the detail fragment. Use the immediate
            // method to avoid the editor picking up the new tunnel while it is still visible.
            fragmentManager.popBackStackImmediate()
        } else if (backStackEntries == 0) {
            // Create and show a new detail fragment.
            fragmentManager.commit {
                add(R.id.detail_container, TunnelDetailFragment())
                setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                addToBackStack(null)
            }
        }
        return true
    }

}