package com.octal.android.mediautils

import android.Manifest
import android.app.Activity.RESULT_CANCELED
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File


class ImagePicker {

    private lateinit var photoPickerLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var cameraPickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var cropperLauncher: ActivityResultLauncher<CropImageContractOptions>
    private lateinit var storagePermissionLauncher: ActivityResultLauncher<String>
    private var imageUri: Uri? = null
    private var context: Context? = null
    var cropOptions: CropImageOptions? = null

    companion object {
        fun with(activity: FragmentActivity, resultUri: (Uri?, File?) -> Unit): ImagePicker =
            ImagePicker().apply {
                photoPickerLauncher =
                    activity.registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                        if (uri != null) {
                            launchCropper(uri)
                        } else {
                            Toast.makeText(
                                context,
                                "Image Not Selected",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                cameraPickerLauncher =
                    activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                        if (imageUri != null) {
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
                storagePermissionLauncher =
                    activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                        if (isGranted) {
                            context?.let {
                                takeImageFormCamera(it)
                            }
                        } else {
                            Toast.makeText(
                                activity.applicationContext,
                                "Please provide permission",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                cropperLauncher =
                    activity.registerForActivityResult(CropImageContract()) { result ->
                        var file:File? = null
                        context?.let { context ->
                            result.uriContent?.let {uri ->
                               file =  FileUtils.fileFromContentUri(context,uri)
                            }
                        }
                        resultUri.invoke(result.uriContent, file)
                    }
            }

        fun with(fragment: Fragment, resultUri: (Uri?, File?) -> Unit) = ImagePicker().apply {
            photoPickerLauncher =
                fragment.registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                    if (uri != null) {
                        launchCropper(uri)
                    } else {
                        Toast.makeText(
                            context,
                            "Image Not Selected",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            cameraPickerLauncher =
                fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                   runCatching {
                       CoroutineScope(Dispatchers.Main).launch {
                           delay(1000)
                           if (imageUri != null) {
                               if (result.resultCode != RESULT_CANCELED) {
                                   launchCropper(imageUri!!)
                                   imageUri = null
                               } else {
                                   Toast.makeText(
                                       context,
                                       "Image Not Captured.",
                                       Toast.LENGTH_SHORT
                                   ).show()
                               }
                           }
                       }
                   }.onFailure {
                       Toast.makeText(
                           context,
                           "Something went wrong.",
                           Toast.LENGTH_SHORT
                       ).show()
                   }
                }
            storagePermissionLauncher =
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
                var file:File? = null
                context?.let { context ->
                    result.uriContent?.let {uri ->
                        file =  FileUtils.fileFromContentUri(context,uri)
                    }
                }
                resultUri.invoke(result.uriContent, file)
            }
        }

    }

    fun getImageFromStorage(context: Context){
        this.context = context
        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    fun getImageFromCamera(context: Context){
        this.context = context
        takeImageFormCamera(context)
    }

    fun isFixedRatioCrop(ratioX: Int, ratioY: Int){
        cropOptions = CropImageOptions(
            guidelines = CropImageView.Guidelines.ON,
            allowRotation = true,
            activityTitle = "Crop",
            fixAspectRatio = true,
            aspectRatioX = ratioX,
            aspectRatioY = ratioY
        )
    }

    private fun takeImageFormCamera(context: Context) {
        runCatching {
            val timeStamp = System.currentTimeMillis()
            val values = ContentValues()
            values.put(
                MediaStore.Images.Media.TITLE,
                "com.octal.mediaUtils_$timeStamp"
            )
            values.put(MediaStore.Images.Media.DESCRIPTION, "From your Camera")
            imageUri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            cameraPickerLauncher.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
            })
        }.onFailure {
            if(it is SecurityException){
                checkPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else{
                Toast.makeText(
                    context,
                    "Something went wrong.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun checkPermission(
        context: Context,
        permission: String,
    ): Boolean {
        val currentAPIVersion = Build.VERSION.SDK_INT
        return if (currentAPIVersion >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                storagePermissionLauncher.launch(permission)
                false
            } else {
                true
            }
        } else {
            true
        }
    }

    private fun showDialog(
        context: Context?,
        msg: String,
        permission: String
    ) {
        val alertBuilder: AlertDialog.Builder = AlertDialog.Builder(context)
        alertBuilder.setCancelable(true)
        alertBuilder.setTitle("Permission necessary")
        alertBuilder.setMessage("$msg permission is necessary")
        alertBuilder.setPositiveButton(
            "Yes"
        ) { _, _ ->
            storagePermissionLauncher.launch(permission)
        }
        val alert: AlertDialog = alertBuilder.create()
        alert.show()
    }

    private fun launchCropper(uri: Uri) {
        cropperLauncher.launch(
            CropImageContractOptions(
                uri,
                cropOptions?:CropImageOptions(
                    guidelines = CropImageView.Guidelines.ON,
                    allowRotation = true,
                    activityTitle = "Crop",
                )
            )
        )
    }

    fun hasImageCaptureBug(): Boolean {
        val devices = ArrayList<String>()
        devices.add("android-devphone1/dream_devphone/dream")
        devices.add("generic/sdk/generic")
        devices.add("vodafone/vfpioneer/sapphire")
        devices.add("tmobile/kila/dream")
        devices.add("verizon/voles/sholes")
        devices.add("google_ion/google_ion/sapphire")
        return devices.contains(Build.BRAND + "/" + Build.PRODUCT + "/" + Build.DEVICE)
    }
}


