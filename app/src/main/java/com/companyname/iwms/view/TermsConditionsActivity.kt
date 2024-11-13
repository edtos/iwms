package com.companyname.iwms.view

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.companyname.iwms.R

class TermsConditionsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_terms_conditions2)
        val btnDecline = findViewById<Button>(R.id.btnDisagree)
        val btnAccept = findViewById<Button>(R.id.btnAgree)


        // Decline Button Click
        btnDecline.setOnClickListener {
            AlertDialog.Builder(this@TermsConditionsActivity)
                .setTitle("Notice")
                .setMessage("To proceed, please accept the Terms and Conditions.")
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()
        }

        // Accept Button Click
        btnAccept.setOnClickListener {
            Toast.makeText(this@TermsConditionsActivity, "Terms Accepted", Toast.LENGTH_SHORT).show()

            // Replace YourNextActivity::class.java with the actual class of the activity you want to navigate to
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }
        }

}
