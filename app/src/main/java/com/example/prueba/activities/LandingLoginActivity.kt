package com.example.prueba.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.prueba.R
import com.example.prueba.databinding.ActivityLandingLoginBinding

class LandingLoginActivity : AppCompatActivity() {
    private lateinit var binding : ActivityLandingLoginBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLandingLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //TODO: VERIFICACIONES DE LOGIN

        binding.loginLandingButton.setOnClickListener {
            startActivity(Intent(baseContext, HomeActivity::class.java))
        }
    }
}