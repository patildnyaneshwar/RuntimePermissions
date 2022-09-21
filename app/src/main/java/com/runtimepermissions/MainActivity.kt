package com.runtimepermissions

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.manager.runtimepermission.RuntimePermission


class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"

    lateinit var runtimePermission: RuntimePermission

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val cameraButton = findViewById<Button>(R.id.cameraButton)
        val cameraLocationButton = findViewById<Button>(R.id.cameraLocationButton)
        val readWriteButton = findViewById<Button>(R.id.readWriteButton)

        runtimePermission = RuntimePermission(this)
        lifecycle.addObserver(runtimePermission)

        cameraButton.setOnClickListener {
            runtimePermission
                .requestPermissions(Manifest.permission.CAMERA)
                .showRationalePermission(
                    true,
                    "Permission Denied",
                    "To capture a profile picture we need this permission. Are you sure, you would like to cancel?"
                )
                .setPositiveButton("Re-try")
                .setNegativeButton("Cancel")
                .setNeutralButton("May be later")
                .setRationaleDialogCancelable(false)
                .result { result ->
                    when (result) {
                        is RuntimePermission.PermissionResult.DetailedPermissionResult -> {
                            result.detailedPermission.forEach {
                                Log.d(TAG, "Permission ${it.key} is allowed ${it.value}")
                            }
                        }
                        is RuntimePermission.PermissionResult.IsPermissionGranted -> {
                            if (result.isGranted) {
                                Log.d(TAG, "Permission Granted")
                                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
                            } else {
                                Log.e(TAG, "Permission Denied")
                                runtimePermission.settingsDialog(
                                    "To Continue give access to app",
                                    "Settings",
                                    true,
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            }
                        }
                        RuntimePermission.PermissionResult.NegativeButtonClicked -> {
                            Log.d(TAG, "Rationale Dialog negative button clicked")
                        }
                        RuntimePermission.PermissionResult.NeutralButtonClicked -> {
                            Log.d(TAG, "Rationale Dialog neutral button clicked")
                        }
                    }
                }

        }

        cameraLocationButton.setOnClickListener {
            runtimePermission
                .requestPermissions(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION)
                .setDetailedPermissionRequired(true)
                .result { result ->
                    when (result) {
                        is RuntimePermission.PermissionResult.DetailedPermissionResult ->{
                            // Check which permission is allowed and which is not
                            // On the basis of it decide what to be done
                            result.detailedPermission.forEach {
                                Log.d(TAG, "Permission ${it.key} is allowed ${it.value}")
                            }

                            if (result.isGranted) {
                                Log.d(TAG, "Permission Granted")
                                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
                            } else {
                                Log.e(TAG, "Permission Denied")
                                runtimePermission.settingsDialog(
                                    "To Continue give access to app",
                                    "Settings",
                                    true,
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            }
                        }
                        is RuntimePermission.PermissionResult.IsPermissionGranted -> {}
                        RuntimePermission.PermissionResult.NegativeButtonClicked -> {
                            Log.d(TAG, "Rationale Dialog negative button clicked")
                        }
                        RuntimePermission.PermissionResult.NeutralButtonClicked -> {
                            Log.d(TAG, "Rationale Dialog neutral button clicked")
                        }
                    }
                }
        }

        readWriteButton.setOnClickListener {
            val requestPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                arrayOf(
                    Manifest.permission.MANAGE_EXTERNAL_STORAGE
                )
            } else {
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            }

            runtimePermission
                .requestPermissions(*requestPermission)
                .showRationalePermission(
                    true,
                    "Permission Denied",
                    "To capture a profile picture we need this permission. Are you sure, you would like to cancel?"
                )
                .setPositiveButton("Re-try")
                .setNegativeButton("Cancel")
                .setNeutralButton("May be later")
                .setRationaleDialogCancelable(true)
                .setDetailedPermissionRequired(true)
                .result { result ->
                    when (result) {
                        is RuntimePermission.PermissionResult.DetailedPermissionResult -> {
                            var isStoragePermissionGranted = false
                            result.detailedPermission.forEach {
                                Log.d(TAG, "Permission ${it.key} is allowed ${it.value}")
                                when(it.key) {
                                    Manifest.permission.MANAGE_EXTERNAL_STORAGE -> {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                                            Environment.isExternalStorageManager()) {
                                            isStoragePermissionGranted = true
                                        }
                                    }
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE -> {
                                        isStoragePermissionGranted = it.value
                                        if (!it.value) {
                                            return@forEach
                                        }
                                    }
                                }
                            }

                            if (!isStoragePermissionGranted) {
                                val action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                                } else {
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                }

                                runtimePermission.settingsDialog(
                                    "To Continue give access to app",
                                    "Settings",
                                    true,
                                    action)
                            } else {
                                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
                            }
                        }
                        is RuntimePermission.PermissionResult.IsPermissionGranted -> {}
                        RuntimePermission.PermissionResult.NegativeButtonClicked -> {
                            Log.d(TAG, "Rationale Dialog negative button clicked")
                        }
                        RuntimePermission.PermissionResult.NeutralButtonClicked -> {
                            Log.d(TAG, "Rationale Dialog neutral button clicked")
                        }

                    }
                }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(runtimePermission)
    }
}