[![](https://jitpack.io/v/aadarshmathur3701/mediaUtils.svg)](https://jitpack.io/#aadarshmathur3701/mediaUtils)

This library use to for multiple media utilities:-

how to import

dependencies {
	        implementation 'com.github.aadarshmathur3701:mediaUtils:1.4.3'
	}

 1. File Utiles
Use for conevert content URI to file
How to Use

 val file = FileUtils.fileFromContentUri(requireContext(), uri, "filePrefix)

 2. Image Picker
Use to pic image for gallery and camera

define this in activity or fragment

private val imagePicker = ImagePicker.with(this(activity or fragment){ uri,file ->

}

how to call
imagePicker.getImageFromCamera(context) // for camera

imagePicker.getImageFromStorage(context) // for storage

3. Video Picker
Use to pic Video for gallery and camera

define this in activity or fragment

private val videoPicker = VideoPicker.with(this(activity or fragment){ uri ->}

how to call
videoPicker.getVideoFromCamera(context) // for camera

videoPicker.getVideoFromStorage(context) // for storage
