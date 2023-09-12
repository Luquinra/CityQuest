package com.example.prueba

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class PhotoDecoracion(private val spacing: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        // Aplica el espacio a la parte inferior del elemento (puedes ajustar esto según tus necesidades)
        outRect.bottom = spacing
    }
}
