package com.example.prueba.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.prueba.databinding.ActivityPuntosBinding

class PuntosActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPuntosBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPuntosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.perfil.setOnClickListener {
            startActivity(Intent(baseContext, PerfilActivity::class.java))
        }

        binding.homeButton.setOnClickListener {
            startActivity(Intent(baseContext, HomeActivity::class.java))
        }

        binding.notificaciones.setOnClickListener {
            startActivity(Intent(baseContext, NotificacionesActivity::class.java))
        }
    }
}