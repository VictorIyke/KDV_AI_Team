package com.example.ortsuperresolutiondemo


import android.graphics.Bitmap
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity(), UserInputFragment.ShowImageListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        supportActionBar?.hide()
        this.window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                            WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.welcome_screen)
        Toast.makeText(this, "Content Created", Toast.LENGTH_SHORT).show()
        
        val buttonBegin: Button = findViewById(R.id.begin)

        buttonBegin.setOnClickListener {
            setContentView(R.layout.activity_main)
        }

    }

    override fun showImage(resolution: String?, bitmap: Bitmap?) {

        var fragmentIdSingle = -1
        when (resolution) {
            "LR" -> fragmentIdSingle = R.id.lowImgView
            "HR" -> fragmentIdSingle = R.id.highImgView
//            "IC" -> fragmentIdSingle = R.id.imageClassView
        }
        if (fragmentIdSingle > 0) {
            val ivf = supportFragmentManager.findFragmentById(fragmentIdSingle) as ImageViewerFragment
            ivf.setImage(bitmap)
        }
    }

}

