package com.example.snaptrail

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object Util {
    private const val REQUEST_CODE_PERMISSIONS = 0
    fun checkAndRequestPermissions(activity: Activity?) {
        if (Build.VERSION.SDK_INT < 23) return

        val permissionsNeeded = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(
                activity!!,
                android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsNeeded.add(android.Manifest.permission.CAMERA)
        }

        if (ContextCompat.checkSelfPermission(
                activity,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsNeeded.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                permissionsNeeded.toTypedArray(),
                REQUEST_CODE_PERMISSIONS
            )
        }
    }
}