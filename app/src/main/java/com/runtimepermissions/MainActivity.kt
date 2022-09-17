package com.runtimepermissions

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.runtimepermissions.permission.RuntimePermission


class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"

    lateinit var runtimePermission: RuntimePermission

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val cameraButton = findViewById<Button>(R.id.cameraButton)
        val cameraLocationButton = findViewById<Button>(R.id.cameraLocationButton)

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
                                runtimePermission.notificationSettingsDialog(
                                    "To Continue give access to app",
                                    "Settings",
                                    true)
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
                                runtimePermission.notificationSettingsDialog(
                                    "To Continue give access to app",
                                    "Settings",
                                    true)
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