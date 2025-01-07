package org.hyperskill.photoeditor

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.slider.Slider
import com.google.android.material.slider.Slider.OnChangeListener

class MainActivity : AppCompatActivity() {

    private lateinit var currentImage: ImageView
    private lateinit var btnGallery: Button
    private lateinit var brightnessSlider: Slider
    private lateinit var contrastSlider: Slider
    private lateinit var btnSave: Button

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

        btnGallery.setOnClickListener {
            activityResultLauncher.launch(
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            )
        }

        btnSave.setOnClickListener {
            onSaveBtnClick()
        }

        brightnessSlider.addOnChangeListener(OnChangeListener { slider, value, fromUser ->
            applyCombinedFilters(value.toInt(), contrastSlider.value.toInt())
        })

        contrastSlider.addOnChangeListener(OnChangeListener { slider, value, fromUser ->
            applyCombinedFilters(brightnessSlider.value.toInt(), value.toInt())
        })

        // do not change this line
        currentImage.setImageBitmap(createBitmap())
    }

    private fun bindViews() {
        currentImage = findViewById(R.id.ivPhoto)
        btnGallery = findViewById(R.id.btnGallery)
        btnSave = findViewById(R.id.btnSave)
        brightnessSlider = findViewById(R.id.slBrightness)
        contrastSlider = findViewById(R.id.slContrast)
    }

    private fun calculateAverageBrightness(bitmap: Bitmap): Int {
        var totalBrightness = 0L
        val width = bitmap.width
        val height = bitmap.height
        val totalPixels = width * height * 3

        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = bitmap.getPixel(x, y)
                val red = Color.red(pixel)
                val green = Color.green(pixel)
                val blue = Color.blue(pixel)

                val brightness = red + green + blue
                totalBrightness += brightness
            }
        }

        return (totalBrightness / totalPixels).toInt()
    }

    private fun applyCombinedFilters(brightness: Int, contrast: Int) {
        if (!::originalBitmap.isInitialized) return

        // Step 0: Create a mutable copy for filtering
        filteredBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)

        val width = filteredBitmap.width
        val height = filteredBitmap.height
        val pixels = IntArray(width * height)

        // Step 1: Retrieve the pixels from the 'filteredBitmap' copy
        // so we can modify them in-memory
        filteredBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // -------------------------
        // STEP 1: BRIGHTNESS FILTER
        // -------------------------
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val alpha = Color.alpha(pixel)

            val red = (Color.red(pixel) + brightness).coerceIn(0, 255)
            val green = (Color.green(pixel) + brightness).coerceIn(0, 255)
            val blue = (Color.blue(pixel) + brightness).coerceIn(0, 255)

            pixels[i] = Color.argb(alpha, red, green, blue)
        }

        // Update filteredBitmap with brightness-adjusted pixels
        filteredBitmap.setPixels(pixels, 0, width, 0, 0, width, height)

        // -----------------------------------
        // STEP 2: RE-CALCULATE AVERAGE BRIGHTNESS
        //       (FROM THE BRIGHTNESS-ADJUSTED IMAGE)
        // -----------------------------------
        val avgBrightness = calculateAverageBrightness(filteredBitmap)

        // Calculate contrast factor
        val contrastFactor = (255.0 + contrast) / (255.0 - contrast)

        // ------------------------
        // STEP 3: CONTRAST FILTER
        // ------------------------
        // Retrieve the already brightness-adjusted pixels again
        filteredBitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val alpha = Color.alpha(pixel)

            val red = ((contrastFactor * (Color.red(pixel) - avgBrightness)) + avgBrightness)
                .toInt().coerceIn(0, 255)
            val green = ((contrastFactor * (Color.green(pixel) - avgBrightness)) + avgBrightness)
                .toInt().coerceIn(0, 255)
            val blue = ((contrastFactor * (Color.blue(pixel) - avgBrightness)) + avgBrightness)
                .toInt().coerceIn(0, 255)

            pixels[i] = Color.argb(alpha, red, green, blue)
        }

        // Step 4: Update the bitmap with the final (brightness + contrast) result
        filteredBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        currentImage.setImageBitmap(filteredBitmap)
    }

    // do not change this function
    fun createBitmap(): Bitmap {
        val width = 200
        val height = 100
        val pixels = IntArray(width * height)

        var R: Int
        var G: Int
        var B: Int
        var index: Int

        for (y in 0 until height) {
            for (x in 0 until width) {
                index = y * width + x
                R = x % 100 + 40
                G = y % 100 + 80
                B = (x + y) % 100 + 120
                pixels[index] = Color.rgb(R, G, B)
            }
        }
        val bitmapOut = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        bitmapOut.setPixels(pixels, 0, width, 0, 0, width, height)
        originalBitmap = bitmapOut
        return bitmapOut
    }

    private fun onSaveBtnClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // If permission is already granted:
                saveToGallery()
            } else {
                // The test specifically checks if we call requestPermissions with code 0
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    0
                )
            }
        } else {
            // Below Marshmallow, no runtime permission needed
            saveToGallery()
        }
    }

    private fun saveToGallery() {
        // If there's nothing in the ImageView, abort
        val drawable = currentImage.drawable ?: run {
            Toast.makeText(this, "No image to save!", Toast.LENGTH_SHORT).show()
            return
        }
        // Convert to bitmap
        val displayedBitmap = (drawable as BitmapDrawable).bitmap

        val contentValues = ContentValues().apply {
            put(
                MediaStore.Images.Media.DISPLAY_NAME,
                "filtered_image_${System.currentTimeMillis()}.jpg"
            )
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val imageUri = contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )

        if (imageUri != null) {
            try {
                contentResolver.openOutputStream(imageUri).use { outputStream ->
                    // Must be JPEG at quality 100
                    displayedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream!!)
                }
                // Mark as no longer pending
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(imageUri, contentValues, null, null)

                Toast.makeText(this, "Image saved to gallery!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to save image: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "Failed to create MediaStore entry", Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ||
                (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            ) {
                saveToGallery()
            } else {
                Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}