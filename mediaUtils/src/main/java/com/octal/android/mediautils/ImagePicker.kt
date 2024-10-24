package com.octal.android.mediautils

import android.Manifest
import android.app.Activity
import android.app.Activity.RESULT_CANCELED
import android.app.AlertDialog
import android.app.Dialog
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.octal.android.mediautils.databinding.ImagePickerDialogBinding
import java.io.InputStream

class ImagePicker {

    private lateinit var photoPickerLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var cameraPickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var cropperLauncher: ActivityResultLauncher<CropImageContractOptions>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private var imageUri: Uri? = null
    private var context: Context? = null

    companion object {
        fun with(activity: FragmentActivity, resultUri: (Uri?) -> Unit): ImagePicker =
            ImagePicker().apply {
                photoPickerLauncher = activity.registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                    when {
                        uri != null -> {
                            launchCropper(uri)
                        }

                        else -> Toast.makeText(context, "Image Not Selected", Toast.LENGTH_SHORT).show()

                    }
                }
                cameraPickerLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                    stopCameraService()
                    imageUri?.let {
                        when {
                            result.resultCode != RESULT_CANCELED -> {
                                launchCropper(imageUri!!)
                            }

                            else -> Toast.makeText(context, "Image Not Captured.", Toast.LENGTH_SHORT).show()

                        }
                    }
                }
                permissionLauncher = activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                    when {
                        isGranted -> {
                            context?.let { takeImageFormCamera(it) }
                        }

                        else -> Toast.makeText(activity.applicationContext, "Please provide permission", Toast.LENGTH_SHORT).show()
                    }
                }
                cropperLauncher = activity.registerForActivityResult(CropImageContract()) { result ->
                    resultUri.invoke(result.uriContent)
                }
            }

        fun with(fragment: Fragment, resultUri: (Uri?) -> Unit) = ImagePicker().apply {
            photoPickerLauncher =
                fragment.registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                    when {
                        uri != null -> {
                            launchCropper(uri)
                        }

                        else -> {
                            Toast.makeText(
                                context,
                                "Image Not Selected",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

            cameraPickerLauncher = fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                stopCameraService()
                when {
                    imageUri != null -> {
                        if (result.resultCode != RESULT_CANCELED) {
                            launchCropper(imageUri!!)
                        } else {
                            Toast.makeText(
                                context,
                                "Image Not Captured.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            permissionLauncher =
                fragment.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                    if (isGranted) {
                        context?.let {
                            takeImageFormCamera(it)
                        }
                    } else {
                        Toast.makeText(
                            fragment.activity,
                            "Please provide permission.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            cropperLauncher = fragment.registerForActivityResult(CropImageContract()) { result ->
                resultUri.invoke(result.uriContent)
            }
        }

        fun getImageFileSizeFromUri(context: Context, imageUri: Uri): Long {
            val contentResolver: ContentResolver = context.contentResolver
            var inputStream: InputStream? = null

            return try {
                inputStream = contentResolver.openInputStream(imageUri)
                inputStream?.available()?.toLong() ?: -1
            } catch (e: Exception) {
                e.printStackTrace()
                -1
            } finally {
                inputStream?.close()
            }
        }
    }

    fun getImage(context: Context) {
        this.context = context
        val imagePickerDialog = Dialog(context)
        val binding = ImagePickerDialogBinding.inflate(LayoutInflater.from(context))
        imagePickerDialog.setContentView(binding.root)
        imagePickerDialog.show()
        binding.selectFromPicker.setOnClickListener {
            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            imagePickerDialog.dismiss()
        }

        binding.selectFromCamera.setOnClickListener {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                if (checkPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    takeImageFormCamera(context)
                }
            } else {
                takeImageFormCamera(context)
            }
            imagePickerDialog.dismiss()
        }
    }

    fun getImageFromStorage(context: Context) {
        this.context = context
        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    fun getImageFromCamera(context: Context) {
        this.context = context
        takeImageFormCamera(context)
    }

    private fun takeImageFormCamera(context: Context) {
        if(checkPermission(context,Manifest.permission.CAMERA)){
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || checkPermission(context, Manifest.permission.POST_NOTIFICATIONS)) {
                captureImage(context)
            }
        }
    }

    private fun captureImage(context: Context){
        startCameraForeground()
        val timeStamp = System.currentTimeMillis()
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "Image_$timeStamp")
        values.put(MediaStore.Images.Media.DESCRIPTION, "From your Camera")
        imageUri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        cameraPickerLauncher.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            this.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        })
    }

    private fun startCameraForeground() {
        val serviceIntent = Intent(context, CameraForegroundService::class.java)
        context?.let { ContextCompat.startForegroundService(it, serviceIntent) }
    }

    private fun stopCameraService(){
        val stopServiceIntent = Intent(context, CameraForegroundService::class.java)
        context?.stopService(stopServiceIntent)
    }

    private fun checkPermission(
        context: Context,
        permission: String,
    ): Boolean {
        val currentAPIVersion = Build.VERSION.SDK_INT
        return if (currentAPIVersion >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(context as Activity, permission)) {
                    showDialog(context, "External storage", permission)
                } else {
                    permissionLauncher.launch(permission)
                }
                false
            } else {
                true
            }
        } else {
            true
        }
    }

    private fun showDialog(context: Context?, msg: String, permission: String) {
        val alertBuilder: AlertDialog.Builder = AlertDialog.Builder(context)
        alertBuilder.setCancelable(true)
        alertBuilder.setTitle("Permission necessary")
        alertBuilder.setMessage("$msg permission is necessary")
        alertBuilder.setPositiveButton("Yes") { _, _ ->
            permissionLauncher.launch(permission)
        }
        val alert: AlertDialog = alertBuilder.create()
        alert.show()
    }

    private fun launchCropper(uri: Uri) {
        cropperLauncher.launch(
            CropImageContractOptions(uri, CropImageOptions(guidelines = CropImageView.Guidelines.ON,
                allowRotation = true,
                activityTitle = "Crop Image",
                fixAspectRatio = true,
                outputCompressQuality = 50)
            )
        )
    }
}


