package com.example.ortsuperresolutiondemo

import android.graphics.Bitmap
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment

class ImageViewerFragment : Fragment(R.layout.fragment_image_viewer) {
    fun setImage(bitmap: Bitmap?) {
        val imgView = view?.findViewById<View>(R.id.imgView) as ImageView
        imgView.setImageBitmap(bitmap)
    }
}