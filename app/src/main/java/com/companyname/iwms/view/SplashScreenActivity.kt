package com.companyname.iwms.view

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.companyname.iwms.R

class SplashScreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        // Delay for 5 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            // Check if app is launched for the first time
            val sharedPreferences: SharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
            val isFirstTime = sharedPreferences.getBoolean("isFirstTime", true)

            if (isFirstTime) {
                // If first time, launch Terms and Conditions
                val editor = sharedPreferences.edit()
                editor.putBoolean("isFirstTime", false)
                editor.apply()

                startActivity(Intent(this, TermsConditionsActivity::class.java))
            } else {
                // If not first time, launch Home screen
                startActivity(Intent(this, HomeActivity::class.java))
            }

            finish() // Close SplashActivity
        }, 5000) // 5 seconds delay
    }
}
