package com.example.ortsuperresolutiondemo

import ai.onnxruntime.*
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.lang.IllegalArgumentException
import java.nio.FloatBuffer
import java.nio.IntBuffer

class UserInputFragment: Fragment(R.layout.user_input_fragment) {

    interface ShowImageListener {
        fun showImage(resolution: String?, bitmap: Bitmap?)
    }

    private val imgWidth: Int = 224
    private val imgHeight: Int = 224
    private val postImgHeight: Int = imgHeight * 3
    private val postImgWidth: Int = imgWidth * 3
    private var showImageListener: ShowImageListener? = null
    private var ortEnv: OrtEnvironment? = null
    private var ortSes: OrtSession? = null
    private var bitmapImage: Bitmap? = null
    private var bitmapScaledCbCr: Bitmap? = null
    private var cb: FloatArray? = null
    private var cr: FloatArray? = null
    private lateinit var buttonRun: Button
    private lateinit var buttonUploadImage: Button


    override fun onAttach(context: Context) {
        super.onAttach(context)
        showImageListener = context as ShowImageListener
    }

    override fun onStart() {
        super.onStart()

        val isOrtInitialized = startOrtSession()
        if (isOrtInitialized) {
            buttonUploadImage = requireView().findViewById(R.id.uploadImage)
            buttonRun = requireView().findViewById(R.id.runModel)
            buttonUploadImage.isEnabled = true
        } else {
            Toast.makeText(requireActivity().applicationContext,
            "Can't Inference", Toast.LENGTH_SHORT).show()

        }
    }

    override fun onDetach() {
        super.onDetach()
        showImageListener = null
        endOrtSession()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        buttonUploadImage = requireView().findViewById(R.id.uploadImage)

        buttonUploadImage.setOnClickListener {
            val intent =
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, RESULT_LOAD_IMAGE)

        }
        buttonRun = requireView().findViewById(R.id.runModel)
        buttonRun.setOnClickListener { runModel(bitmapImage) }
        }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RESULT_LOAD_IMAGE && data != null) {
            val imageUri = data.data
            val imageStream = requireActivity().contentResolver.openInputStream(
                imageUri!!
            )

            val imageBitmap = BitmapFactory.decodeStream(imageStream)
            bitmapImage = imageBitmap
            bitmapScaledCbCr = Bitmap.createScaledBitmap(bitmapImage!!, postImgWidth, postImgHeight,
                                                                                        true)
            showImageListener!!.showImage("LR", bitmapScaledCbCr)
            buttonRun.isEnabled = true
        } else {
            Toast.makeText(requireActivity().applicationContext, "No Picture Selected",
                            Toast.LENGTH_LONG).show()
        }
    }

    private fun startOrtSession(): Boolean {
        var success = false
        if (ortEnv != null && ortSes != null) {
            success = true
        }
        if (ortEnv == null) {
            ortEnv = OrtEnvironment.getEnvironment()
        }
        if (ortEnv != null && ortSes == null) {

            val modelAsArray = loadModel()

            modelAsArray?.let {
                ortSes = ortEnv!!.createSession(it)
                if (ortSes != null) {
                    success = true
                }
            }
        }
        return success
    }

    private fun runModel(bitmapInput: Bitmap?) {
        var bitmapProcessed: Bitmap? = null
        if (ortSes == null || ortEnv == null) {
            Toast.makeText(requireActivity().applicationContext, "You should load mode first",
                Toast.LENGTH_SHORT).show()
            startOrtSession()
        }
        if (ortSes != null && ortEnv != null) {
            val bitmapScaled = Bitmap.createScaledBitmap(bitmapInput!!, imgWidth, imgHeight,
                true)
            val imageAsFloatArray = imageToFloatArray(bitmapScaled)
            val inputName = ortSes!!.inputNames.iterator().next()
            val inTensorShape = longArrayOf(1, 1, imgHeight.toLong(), imgWidth.toLong())
            val inputs = OnnxTensor.createTensor(
                ortEnv,
                FloatBuffer.wrap(imageAsFloatArray), inTensorShape
            )
            val ortInput: MutableMap<String, OnnxTensor> = HashMap()
            ortInput[inputName] = inputs
            val ortOutput = ortSes!!.run(ortInput)
            val outputTensor = ortOutput[0] as OnnxTensor
            val outputValue = outputTensor.floatBuffer.array()
            val outTensorShape = outputTensor.info.shape
            bitmapProcessed = floatArrayToImage(
                outputValue, outTensorShape[3].toInt(),
                outTensorShape[2].toInt()
            )
        }
        if (bitmapProcessed != null) {
            showImageListener!!.showImage("HR", bitmapProcessed)
        }
    }


    private fun endOrtSession() {
        if (ortSes!= null) ortSes?.close()
        if (ortEnv!= null) ortEnv?.close()
        ortSes = null
        ortEnv = null
        Toast.makeText(requireActivity().applicationContext, "Done", Toast.LENGTH_SHORT).show()
    }

    private fun loadModel(): ByteArray? {
        var result: ByteArray?
        val resManager = resources
        try {
            resManager.let {
                val inputStream = it.openRawResource(R.raw.super_resnet12)
                val outputStream = ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                var bytesRead = inputStream.read(buffer)
                while (bytesRead >= 0) {
                    outputStream.write(buffer, 0, bytesRead)
                    bytesRead = inputStream.read(buffer)
                }
                result = outputStream.toByteArray()
            }
        }
        catch (e: IOException) {
            e.printStackTrace()
            result = null
        }
        return result
    }

    private fun imageToFloatArray(bitmap: Bitmap): FloatArray{
        val width = bitmap.width
        val height = bitmap.height
        val numPixels = width * height
        val pixelAsIntArray = IntArray(numPixels)
        val postPixelArray = IntArray(postImgHeight * postImgWidth)

        bitmap.getPixels(pixelAsIntArray, 0, width, 0, 0, width, height)
        bitmapScaledCbCr!!.getPixels(postPixelArray, 0, postImgWidth, 0, 0,
                                                                postImgWidth, postImgHeight)

        val y = FloatArray(numPixels)
        val cbHere = FloatArray(postImgWidth * postImgHeight)
        val crHere = FloatArray(postImgWidth * postImgHeight)
        var iPixel = 0
        for (row in 0 until height) {
            for (col in 0 until width ) {
                val pixel = pixelAsIntArray[iPixel]

                y[iPixel] = pixelRGBToYCbCr(pixel, "y")
                ++iPixel

            }
        }

        iPixel = 0
        for (row in 0 until postImgHeight) {
            for (col in 0 until postImgWidth) {
                val pixel = postPixelArray[iPixel]


                cbHere[iPixel] = pixelRGBToYCbCr(pixel, "cb")
                crHere[iPixel] = pixelRGBToYCbCr(pixel, "cr")
                ++iPixel
            }
        }
        cb = cbHere
        cr = crHere
        return y
    }

    private fun floatArrayToImage(array: FloatArray, outWidth: Int, outHeight: Int): Bitmap{
        val numPixels = outWidth * outHeight
        val pixelsAsIntArray = IntArray(numPixels)
        val cbHere = cb!!
        val crHere = cr!!
        var iPixel = 0
        for (row in 0 until outHeight) {
            for (col in 0 until outWidth) {


                pixelsAsIntArray[iPixel] =  pixelYCbCrToRGB(array[iPixel], cbHere[iPixel], crHere[iPixel])

                ++iPixel
            }
        }
        val bitmapOutput = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        bitmapOutput.copyPixelsFromBuffer(IntBuffer.wrap(pixelsAsIntArray))


        return bitmapOutput
    }

    private fun pixelRGBToYCbCr(pixel: Int, mode: String): Float {
        val red =
            (pixel shr 16 and 0xFF)
        val green =
            (pixel shr 8 and 0xFF)
        val blue =
            (pixel and 0xFF)

        val y = (0.299.toFloat() * red +
                0.587.toFloat() * green +
                0.114.toFloat() * blue) / 255F

        val cb = (((-0.168935) * red +
                (-0.331665) * green +
                0.50059 * blue) + 128).toFloat()
        val cr = ((0.499813 * red +
                (-0.418531) * green +
                (-0.081282) * blue) + 128).toFloat()

        return when (mode) {
            "y" -> y
            "cb" -> cb
            "cr" -> cr


            else -> {throw IllegalArgumentException("Only use specified modes")}
        }

    }

    private fun pixelYCbCrToRGB(pixel: Float, cb: Float, cr: Float): Int {
        /*
        Function to turn pixels
         */
        var y = (pixel * 255F).toInt()

        y = y.coerceAtMost(255).coerceAtLeast(0)

        val red = (y + 1.4025 * (cr-0x80)).toInt()
            .coerceAtMost(255).coerceAtLeast(0)

        val green = (y + (-0.34373) * (cb-0x80) +
                (-0.7144) * (cr-0x80)).toInt()
            .coerceAtMost(255).coerceAtLeast(0)

        val blue = (y + 1.77200 * (cb-0x80)).toInt()
            .coerceAtMost(255).coerceAtLeast(0)

        return  ((0xff shl 24) or
                 (0xff and blue shl 16) or
                 (0xff and green shl 8) or
                 (0xff and red))
    }

    companion object {
        private const val RESULT_LOAD_IMAGE = 1

    }



}