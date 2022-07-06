package com.example.ortsuperresolutiondemo

import ai.onnxruntime.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import java.io.ByteArrayOutputStream
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
    private var ortSesSuperRes: OrtSession? = null
    private var ortSesImageClass: OrtSession? = null
    private var bitmapImage: Bitmap? = null
    private var bitmapScaledCbCr: Bitmap? = null
    private var cb: FloatArray? = null
    private var cr: FloatArray? = null
    private lateinit var buttonRun: Button
    private lateinit var buttonUploadImage: Button
//    private lateinit var buttonImageClass: Button


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
//            buttonImageClass = requireView().findViewById(R.id.imageClassButton)
            buttonUploadImage.isEnabled = true
        } else if (ortSesSuperRes == null){
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
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, RESULT_LOAD_IMAGE)

        }
        buttonRun = requireView().findViewById(R.id.runModel)
        buttonRun.setOnClickListener { runSuperResModel(bitmapImage) }
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

        if (ortEnv == null) {
            ortEnv = OrtEnvironment.getEnvironment()
        }
        if (ortEnv != null && ortSesSuperRes == null && ortSesImageClass == null) {
            var modelAsArray = loadModel()[0]
            modelAsArray?.let {
                ortSesSuperRes = ortEnv!!.createSession(it)
            }

            modelAsArray = loadModel()[1]
            modelAsArray?.let {
                ortSesImageClass = ortEnv!!.createSession(it)
            }

            if (ortSesSuperRes != null && ortSesImageClass != null) {
                success = true
            }
        }
        return success
    }

    private fun runSuperResModel(bitmapInput: Bitmap?) {
        var bitmapProcessed: Bitmap? = null
        if (ortSesSuperRes == null || ortEnv == null) {
            Toast.makeText(requireActivity().applicationContext, "You should load mode first",
                Toast.LENGTH_SHORT).show()
            startOrtSession()
        }
        if (ortSesSuperRes != null && ortEnv != null) {
            val bitmapScaled = Bitmap.createScaledBitmap(bitmapInput!!, imgWidth, imgHeight,
                true)
            val imageAsFloatArray = imageToFloatArray(bitmapScaled)
            val inputName = ortSesSuperRes!!.inputNames.iterator().next()
            val inTensorShape = longArrayOf(1, 1, imgHeight.toLong(), imgWidth.toLong())
            val inputs = OnnxTensor.createTensor(
                ortEnv,
                FloatBuffer.wrap(imageAsFloatArray), inTensorShape
            )
            val ortInput: MutableMap<String, OnnxTensor> = HashMap()
            ortInput[inputName] = inputs
            val ortOutput = ortSesSuperRes!!.run(ortInput)
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
//            buttonImageClass.isEnabled = true
//
//            buttonImageClass.setOnClickListener {
//                runImageClassModel(bitmapProcessed)
//                MainActivity().setContentView(R.layout.image_class_view)
//            }

        }
    }

//    private fun runImageClassModel(bitmapInput: Bitmap?) {
//        val bitmapScaled = Bitmap.createScaledBitmap(bitmapInput!!, 224, 224, true)
//
//        showImageListener!!.showImage("IC", bitmapScaled)
//
//
//    }

    private fun endOrtSession() {
        if (ortSesSuperRes!= null) ortSesSuperRes?.close()
        if (ortEnv!= null) ortEnv?.close()
        ortSesSuperRes = null
        ortEnv = null
        Toast.makeText(requireActivity().applicationContext, "Done", Toast.LENGTH_SHORT).show()
    }

    private fun loadModel(): MutableList<ByteArray?> {
        val result = mutableListOf<ByteArray?>()
        val resManager = resources

        resManager.let {
            val inputStream = it.openRawResource(R.raw.super_resnet12)
            val outputStream = ByteArrayOutputStream()
            val buffer =ByteArray(4096)
            var bytesRead = inputStream.read(buffer)
            while (bytesRead >= 0) {
                outputStream.write(buffer, 0, bytesRead)
                bytesRead = inputStream.read(buffer)
            }
            result.add(outputStream.toByteArray())

            val inputStream2 = it.openRawResource(R.raw.mobilenet_v2_float)
            val outputStream2 = ByteArrayOutputStream()
            val buffer2 =ByteArray(4096)
            var bytesRead2 = inputStream2.read(buffer2)
            while (bytesRead2 >= 0) {
                outputStream2.write(buffer2, 0, bytesRead2)
                bytesRead2 = inputStream2.read(buffer2)
            }
            result.add(outputStream2.toByteArray())
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
                val red =
                    (pixel shr 16 and 0xff)
                val green =
                    (pixel shr 8 and 0xff)
                val blue =
                    (pixel and 0xff)

                y[iPixel] = (0.299.toFloat() * red +
                             0.587.toFloat() * green +
                             0.114.toFloat() * blue) / 255F
                ++iPixel

            }
        }
        iPixel = 0
        for (row in 0 until postImgHeight) {
            for (col in 0 until postImgWidth) {
                val pixel = postPixelArray[iPixel]
                val red =
                    (pixel shr 16 and 0xFF)
                val green =
                    (pixel shr 8 and 0xFF)
                val blue =
                    (pixel and 0xFF)

                cbHere[iPixel] = (((-0.168935) * red +
                        (-0.331665) * green +
                        0.50059 * blue) + 128).toFloat()
                crHere[iPixel] = ((0.499813 * red +
                        (-0.418531) * green +
                        (-0.081282) * blue) + 128).toFloat()
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

                var y = (array[iPixel] * 255F).toInt()

                y = y.coerceAtMost(255).coerceAtLeast(0)



                val red = (y + 1.4025 * (crHere[iPixel]-0x80)).toInt()
                    .coerceAtMost(255).coerceAtLeast(0)

                val green = (y + (-0.34373) * (cbHere[iPixel]-0x80) +
                                      (-0.7144) * (crHere[iPixel]-0x80)).toInt()
                    .coerceAtMost(255).coerceAtLeast(0)

                val blue = (y + 1.77200 * (cbHere[iPixel]-0x80)).toInt()
                    .coerceAtMost(255).coerceAtLeast(0)

                pixelsAsIntArray[iPixel] =  (0xff shl 24) or
                                            (0xff and blue shl 16) or
                                            (0xff and green shl 8) or
                                            (0xff and red)

                ++iPixel
            }
        }
        val bitmapOutput = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        bitmapOutput.copyPixelsFromBuffer(IntBuffer.wrap(pixelsAsIntArray))


        return bitmapOutput
    }

//    private fun convertBitmapToFloatArray(bitmap: Bitmap): FloatArray {
//        val width = bitmap.width
//        val height = bitmap.height
//        val numPixels = width * height
//        val pixelsAsIntArray = IntArray(numPixels)
//        bitmap.getPixels(pixelsAsIntArray, 0, width, 0, 0, width, height)
//        val buffer = FloatArray(numPixels * 3)
//        val mean = arrayListOf(0.079f, 0.05f, 0f) + 0.406f
//        val std = arrayListOf(0.005f, 0f, 0.001f) + 0.224f
//        val scale = 1f/255f
//        var iPixel = 0
//        for (y in 0 until height) {
//            for (x in 0 until width) {
//                val pixel = pixelsAsIntArray[iPixel]
//                // storing data as NCHW
//                buffer[iPixel] = (((pixel shr 16 and 0xFF) * scale) - mean[0]) / std[0]
//                buffer[numPixels + iPixel] = (((pixel shr 8 and 0xFF) * scale) - mean[1]) / std[1]
//                buffer[2 * numPixels + iPixel] = (((pixel and 0xFF) * scale) - mean[2]) / std[2]
//                ++iPixel
//            }
//        }
//        return buffer
//    }

    companion object {
        private const val RESULT_LOAD_IMAGE = 1
    }



}