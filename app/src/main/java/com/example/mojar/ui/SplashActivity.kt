package com.example.mojar.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.mojar.R
import com.google.firebase.FirebaseApp

/**
 * Entry point of the application. Displays the brand animation (Lottie)
 * and initializes background services.
 */
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialize Firebase services early in the lifecycle
        FirebaseApp.initializeApp(this)

        super.onCreate(savedInstanceState)

        // Set the layout containing the LottieAnimationView
        setContentView(R.layout.activity_splash)

        /**
         * Splash Delay Logic:
         * We use a delay to allow the Lottie animation to play fully
         * before transitioning to the next screen.
         */
        val splashTimeout = 2500L // 2.5 Seconds

        // Use Handler with Looper.getMainLooper() to ensure the transition runs on the UI thread
        Handler(Looper.getMainLooper()).postDelayed({

            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)

            finish()

        }, splashTimeout)
    }
}