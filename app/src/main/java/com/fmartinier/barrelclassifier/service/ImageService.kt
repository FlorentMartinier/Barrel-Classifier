package com.fmartinier.barrelclassifier.service

import android.animation.ValueAnimator
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.provider.MediaStore
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import androidx.core.animation.addListener
import androidx.core.content.FileProvider
import coil.load
import java.io.File
import java.io.FileOutputStream

class ImageService {

    /**
     * Affichage d'une image en plein écran, avec animation d'ouverture.
     */
    fun showImageFullscreen(context: Context, path: String) {
        val dialog = Dialog(context, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen)

        val imageView = ImageView(context)
        imageView.setBackgroundColor(Color.TRANSPARENT)
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER

        imageView.load(File(path)) {
            crossfade(false)
            listener(onSuccess = { _, _ ->
                imageView.scaleX = 0f
                imageView.scaleY = 0f

                val animator = ValueAnimator.ofFloat(0f, 1f)
                animator.duration = 400
                animator.interpolator = DecelerateInterpolator()

                animator.addUpdateListener { animation ->
                    val value = animation.animatedValue as Float

                    imageView.scaleX = value
                    imageView.scaleY = value

                    val alpha = (value * 255).toInt()
                    imageView.setBackgroundColor(Color.argb(alpha, 0, 0, 0))
                }
                animator.start()
            })
        }

        dialog.setContentView(imageView)
        dialog.show()

        imageView.setOnClickListener {
            val animator = ValueAnimator.ofFloat(1f, 0f)
            animator.duration = 300
            animator.interpolator = DecelerateInterpolator()

            animator.addUpdateListener { animation ->
                val value = animation.animatedValue as Float
                imageView.scaleX = value
                imageView.scaleY = value

                val alpha = (value * 255).toInt()
                imageView.setBackgroundColor(Color.argb(alpha, 0, 0, 0))
            }

            animator.addListener(onEnd = {
                dialog.dismiss()
            })
            animator.start()
        }
    }

    fun copyImageToInternalStorage(uri: Uri, activity: Activity): String {
        val file = createImageFile(activity)
        val input = activity.contentResolver.openInputStream(uri)
        val output = FileOutputStream(file)

        input?.copyTo(output)
        input?.close()
        output.close()

        return file.absolutePath
    }

    fun createImageFile(activity: Activity): File {
        val storageDir = activity.filesDir
        val file = File.createTempFile(
            "img_${System.currentTimeMillis()}",
            ".jpg",
            storageDir
        )
        return file
    }

    fun takePhoto(activity: Activity, photoFile: File): Intent {
        val photoURI = FileProvider.getUriForFile(
            activity,
            "${activity.packageName}.fileprovider",
            photoFile
        )

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
        return intent
    }
}