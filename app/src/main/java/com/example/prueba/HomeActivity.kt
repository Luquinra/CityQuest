package com.example.prueba

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import com.example.prueba.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

         binding.notificaciones.setOnClickListener{
            startActivity(Intent(baseContext, NotificacionesActivity::class.java))
        }

        binding.perfil.setOnClickListener {
            startActivity(Intent(baseContext, PerfilActivity::class.java))
        }

        binding.tiendaPuntos.setOnClickListener {
            startActivity(Intent(baseContext, PuntosActivity::class.java))
        }
    }
}