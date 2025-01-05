package org.hyperskill.photoeditor

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.slider.Slider
import com.google.android.material.slider.Slider.OnChangeListener


class MainActivity : AppCompatActivity() {

    private lateinit var currentImage: ImageView
    private lateinit var btn: Button
    private lateinit var brightnessSlider: Slider

    private lateinit var originalBitmap: Bitmap
    private lateinit var filteredBitmap: Bitmap

    private val activityResultLauncher = registerForActivityResult(
        StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val photoUri = result.data?.data ?: return@registerForActivityResult
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, photoUri)

            originalBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            currentImage.setImageBitmap(originalBitmap)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindViews()

        btn.setOnClickListener {
            activityResultLauncher.launch(
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            )
        }

        brightnessSlider.addOnChangeListener(OnChangeListener { slider, value, fromUser ->
            applyBrightnessFilter(value.toInt())
        })

        //do not change this line
        currentImage.setImageBitmap(createBitmap())
    }

    private fun bindViews() {
        currentImage = findViewById(R.id.ivPhoto)
        btn = findViewById(R.id.btnGallery)
        brightnessSlider = findViewById(R.id.slBrightness)
    }

    private fun applyBrightnessFilter(brightness: Int) {
//      ------- optimized version ----------
//        if (!::originalBitmap.isInitialized) return
//
//        filteredBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
//
//        val canvas = Canvas(filteredBitmap)
//        val paint = Paint()
//
//        val colorMatrix = ColorMatrix()
//        colorMatrix.set(
//            floatArrayOf(
//                1f, 0f, 0f, 0f, brightness.toFloat(),
//                0f, 1f, 0f, 0f, brightness.toFloat(),
//                0f, 0f, 1f, 0f, brightness.toFloat(),
//                0f, 0f, 0f, 1f, 0f
//            )
//        )
//
//        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
//        canvas.drawBitmap(originalBitmap, 0f, 0f, paint)
//
//        currentImage.setImageBitmap(filteredBitmap)


        if (!::originalBitmap.isInitialized) return

        filteredBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)

        for (x in 0 until filteredBitmap.width) {
            for (y in 0 until filteredBitmap.height) {
                val pixel = originalBitmap.getPixel(x, y)

                val alpha = Color.alpha(pixel)
                val red = (Color.red(pixel) + brightness).coerceIn(0, 255)
                val green = (Color.green(pixel) + brightness).coerceIn(0, 255)
                val blue = (Color.blue(pixel) + brightness).coerceIn(0, 255)

                val newPixel = Color.argb(alpha, red, green, blue)
                filteredBitmap.setPixel(x, y, newPixel)
            }
        }

        currentImage.setImageBitmap(filteredBitmap)
    }

    // do not change this function
    fun createBitmap(): Bitmap {
        val width = 200
        val height = 100
        val pixels = IntArray(width * height)
        // get pixel array from source

        var R: Int
        var G: Int
        var B: Int
        var index: Int

        for (y in 0 until height) {
            for (x in 0 until width) {
                // get current index in 2D-matrix
                index = y * width + x
                // get color
                R = x % 100 + 40
                G = y % 100 + 80
                B = (x + y) % 100 + 120

                pixels[index] = Color.rgb(R, G, B)

            }
        }
        // output bitmap
        val bitmapOut = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        bitmapOut.setPixels(pixels, 0, width, 0, 0, width, height)
        originalBitmap = bitmapOut
        return bitmapOut
    }
}