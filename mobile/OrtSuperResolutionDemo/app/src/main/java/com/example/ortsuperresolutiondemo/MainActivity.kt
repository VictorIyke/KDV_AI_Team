package com.example.ortsuperresolutiondemo


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import ai.onnxruntime.*


class MainActivity : AppCompatActivity(), UserInputFragment.ShowImageListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        supportActionBar?.hide()
        this.window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                            WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.welcome_screen)

        val buttonBegin: Button = findViewById(R.id.begin)
        buttonBegin.setOnClickListener {
            if (allPermissionsGranted()) {
                setContentView(R.layout.activity_main)
            } else {
                ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
                )
            }
        }

    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                setContentView(R.layout.activity_main)
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
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

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}

