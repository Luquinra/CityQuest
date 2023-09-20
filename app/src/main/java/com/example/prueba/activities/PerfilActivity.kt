package com.example.prueba.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prueba.adapters.PhotoAdapter
import com.example.prueba.PhotoDecoracion
import com.example.prueba.items.PhotoItem
import com.example.prueba.R
import com.example.prueba.databinding.ActivityPerfilBinding

class PerfilActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPerfilBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPerfilBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val recyclerView = findViewById<RecyclerView>(R.id.photoRecyclerView)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL // Opcional: Cambia la orientación a horizontal
        recyclerView.layoutManager = layoutManager

        val spacingInPixels = resources.getDimensionPixelSize(R.dimen.spacing) // Define el valor de espaciado en dimens.xml
        recyclerView.addItemDecoration(PhotoDecoracion(spacingInPixels))

        val photos = listOf(
            PhotoItem("https://tse2.mm.bing.net/th?id=OIP.zHomk1pL8vL0R9gu-3i0NAHaD7&pid=Api&P=0&h=180"),
            PhotoItem("https://www.colombia-travels.com/wp-content/uploads/adobestock-266299444-1.jpeg"),
            PhotoItem("https://interbogotahotel.com/wp-content/uploads/2020/10/turismo-en-bogota-04.png")
            // Agrega más fotos aquí
        )

        val adapter = PhotoAdapter(photos)
        recyclerView.adapter = adapter

        binding.homeButton.setOnClickListener {
            startActivity(Intent(baseContext, HomeActivity::class.java))
        }

        binding.tiendaPuntos.setOnClickListener {
            startActivity(Intent(baseContext, PuntosActivity::class.java))
        }

        binding.notificaciones.setOnClickListener {
            startActivity(Intent(baseContext, NotificacionesActivity::class.java))
        }

    }
}