package com.octal.android.imagepicker

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.octal.android.mediautils.ImagePicker
import com.octal.android.mediautils.VideoPicker

class MainActivity : AppCompatActivity() {

    var imageView: ImageView? = null
    val imagePicker = ImagePicker.with(this){ uri: Uri? ->
        imageView?.setImageURI(uri)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        imageView = findViewById(R.id.ivImage)
        findViewById<Button>(R.id.button).setOnClickListener {
            imagePicker.getImageFromCamera(this)
        }
    }
}