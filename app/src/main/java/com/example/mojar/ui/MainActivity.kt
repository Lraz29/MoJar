package com.example.mojar.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.mojar.fragments.AccountsFragment
import com.example.mojar.fragments.DashboardFragment
import com.example.mojar.R
import com.example.mojar.fragments.ReportsFragment
import com.example.mojar.fragments.SavingsFragment
import com.example.mojar.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

/**
 * The main container for the application after a successful login.
 * Handles navigation between primary features via BottomNavigationView.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        // Force Light Mode for consistent UI/UX design
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        super.onCreate(savedInstanceState)

        // 1. Initialize Firebase Auth to check session status
        auth = FirebaseAuth.getInstance()

        // 2. Session Guard: Ensure user is authenticated before showing the UI
        if (auth.currentUser == null) {
            goToLogin()
            return
        }

        // 3. Initialize ViewBinding and Set Content View
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set default fragment (Dashboard) on first launch
        if (savedInstanceState == null) {
            replaceFragment(DashboardFragment())
        }

        // 4. Bottom Navigation Menu Logic
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    replaceFragment(DashboardFragment())
                    true
                }

                R.id.nav_reports -> {
                    replaceFragment(ReportsFragment())
                    true
                }

                R.id.nav_savings -> {
                    replaceFragment(SavingsFragment())
                    true
                }

                R.id.nav_accounts -> {
                    replaceFragment(AccountsFragment())
                    true
                }

                else -> false
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            ) // Smooth transition
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}