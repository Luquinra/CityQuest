package com.example.prueba.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prueba.adapters.NotificationAdapter
import com.example.prueba.items.NotificationItem
import com.example.prueba.R
import com.example.prueba.databinding.ActivityNotificacionesBinding

class NotificacionesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNotificacionesBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificacionesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val notificationList = listOf(
            NotificationItem("Nuevo seguidor", "Laura te ha seguido", true, "Hace 5 minutos"),
            NotificationItem("Nueva reacción", "María le ha dado like a tu publicación", false, "Hace 10 minutos"),
            NotificationItem("Nuevo seguidor", "María te ha seguido", false, "Hace 11 minutos")
            // Agrega más elementos de notificación según sea necesario
        )

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.adapter = NotificationAdapter(notificationList)
        recyclerView.layoutManager = LinearLayoutManager(this)

        binding.perfil.setOnClickListener {
            startActivity(Intent(baseContext, PerfilActivity::class.java))
        }

        binding.tiendaPuntos.setOnClickListener {
            startActivity(Intent(baseContext, PuntosActivity::class.java))
        }

        binding.homeButton.setOnClickListener {
            startActivity(Intent(baseContext, HomeActivity::class.java))
        }
    }
}
