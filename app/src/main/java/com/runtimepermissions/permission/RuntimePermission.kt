package com.runtimepermissions.permission

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.NonNull
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

/**
 * Runtime permissions to request and check System permissions for apps targeting Android M (API &gt;= 23).
 *
 * Class inherits [LifecycleEventObserver] it's require to add [Lifecycle.addObserver] in the receiver
 */
class RuntimePermission constructor(@NonNull private val activity: FragmentActivity): LifecycleEventObserver {
    
    private var permissionsResult: ActivityResultLauncher<Array<String>>? = null
    private var deniedPermissionForRationale: String = ""
    private val requiredPermissions = mutableListOf<String>()
    private var rationale = false
    private var title = ""
    private var description = ""
    private var positiveText = ""
    private var negativeText = ""
    private var neutralText = ""
    private var neutralButtonVisibility = View.GONE
    private var isRationaleDialogCancelable = true
    private var isDetailedPermissionRequired = false

    /**
     * Callback to receive the results of {@code RuntimePermission.requestPermissions()} call.
     */
    private var callback: (PermissionResult) -> Unit = {}

    /**
     * As the initialization will be done in OnCreate of receiver.
     * It's possible to use same instance to ask another permission.
     * Therefore we don't require same instance values to be reflected instead use new instance/values which are assigned.
     * Hence after each callback, it's required to clear old instance/values
     * */
    private fun clear() {
        requiredPermissions.clear()
        rationale = false
        title = ""
        description = ""
        positiveText = ""
        negativeText = ""
        neutralText = ""
        neutralButtonVisibility = View.GONE
        isRationaleDialogCancelable = true
        isDetailedPermissionRequired = false
    }

    /**
     * Request a set of permissions
     * @param permissions set of permissions to be requested.
     * @return This RuntimePermission object to allow for chaining of calls to set methods
     * @see Manifest.permission
     */
    fun requestPermissions(vararg permissions: String): RuntimePermission {
        requiredPermissions.addAll(permissions)
        return this
    }

    /**
     * Request whether Rationale permission dialog is required
     *
     * @param rationale Decides whether Rationale permission need to show or not
     * @param title Title for dialog is required
     * @param description Description for dialog is required
     * @return This RuntimePermission object to allow for chaining of calls to set methods
     *
     * @see FragmentActivity.shouldShowRequestPermissionRationale
     * */
    fun showRationalePermission(
        rationale: Boolean,
        @NonNull title: String,
        @NonNull description: String
    ): RuntimePermission {
        this.rationale = rationale
        this.title = title
        this.description = description
        return this
    }

    /**
     * Positive Button for Rationale dialog
     *
     * @param positiveText set text to Dialog's positive button
     * @return This RuntimePermission object to allow for chaining of calls to set methods
     *
     * @see RuntimePermission.showRationalePermission
     * @see AlertDialog.Builder.setPositiveButton
     * */
    fun setPositiveButton(@NonNull positiveText: String): RuntimePermission {
        this.positiveText = positiveText
        return this
    }

    /**
     * Negative Button for Rationale dialog
     *
     * @param negativeText set text to Dialog's negative button
     * @return This RuntimePermission object to allow for chaining of calls to set methods
     *
     * @see RuntimePermission.showRationalePermission
     * @see AlertDialog.Builder.setNegativeButton
     * */
    fun setNegativeButton(@NonNull negativeText: String): RuntimePermission {
        this.negativeText = negativeText
        return this
    }

    /**
     * Neutral Button for Rationale dialog
     *
     * @param neutralText set text to Dialog's neutral button
     * @param visibility whether to show neutral button or not. Default is set to #View.VISIBLE
     * #neutralButtonVisibility, default set to #View.GONE
     * @return This RuntimePermission object to allow for chaining of calls to set methods
     *
     * @see RuntimePermission.showRationalePermission
     * @see AlertDialog.Builder.setNeutralButton
     * */
    fun setNeutralButton(
        @NonNull neutralText: String,
        @NonNull visibility: Int = View.VISIBLE
    ): RuntimePermission {
        this.neutralText = neutralText
        this.neutralButtonVisibility = visibility
        return this
    }

    /**
     * Sets whether the Rational Dialog is cancelable or not
     * @param isCancelable Boolean value passed to Rationale Dialog. Default is true
     * #isRationaleDialogCancelable, default set to true
     * @return This RuntimePermission object to allow for chaining of calls to set methods
     *
     * @see AlertDialog.Builder.setCancelable
     * */
    fun setRationaleDialogCancelable(isCancelable: Boolean = true): RuntimePermission {
        this.isRationaleDialogCancelable = isCancelable
        return this
    }

    /**
     * Sets whether to return detailed permission which are been requested in #callback
     * @param isDetailedPermissionRequired Default is false
     * #isDetailedPermissionRequired, default set to false
     *
     * @return This RuntimePermission object to allow for chaining of calls to set methods
     * */
    fun setDetailedPermissionRequired(isDetailedPermissionRequired: Boolean = false): RuntimePermission {
        this.isDetailedPermissionRequired = isDetailedPermissionRequired
        return this
    }

    /**
     * Checks whether the requested permission is Granted or not
     * @return isGranted, #true if requested permission are granted. #false if not
     * */
    private fun checkSelfPermissionGranted(): Boolean {
        var isGranted = true
        requiredPermissions.forEach {
            if (activity.checkSelfPermission(it) == PackageManager.PERMISSION_DENIED) {
                deniedPermissionForRationale = it
                isGranted = false
                return@forEach
            }
        }

        /**
         * #deniedPermissionForRationale should not be empty.
         * Because we use it #result.shouldShowRequestPermissionRationale to show Rationale request if user deny the permission.
         * In case permission is granted #result.shouldShowRequestPermissionRationale is always false
         * */
        if (deniedPermissionForRationale.isEmpty()) {
            deniedPermissionForRationale = requiredPermissions[0]
        }
        return isGranted
    }

    /**
     * @return callback to the requested receiver with respective values.
     * Once the #callback is sent to receiver #clear() the set values.
     *
     * [Explanation] If #isDetailedPermissionRequired is true,
     * In this case if #checkSelfPermissionGranted() is true, #DetailedPermissionResult() will send true value to requested permissions.
     * In case of #shouldShowRequestPermissionRationale() it will send false value to the last permission, If #rationale is false
     * */
    fun result(callback: (PermissionResult) -> Unit) {
        this.callback = callback

        when {
            checkSelfPermissionGranted() -> {
                if (isDetailedPermissionRequired) {
                    val detailedPermission = mutableMapOf<String, Boolean>()
                    requiredPermissions.forEach {
                        detailedPermission[it] = true
                    }

                    this.callback(PermissionResult.DetailedPermissionResult(true, detailedPermission.entries))
                } else {
                    this.callback(PermissionResult.IsPermissionGranted(true))
                }
                clear()
            }
            activity.shouldShowRequestPermissionRationale(deniedPermissionForRationale) -> {
                if (rationale) {
                    displayRationaleDialog()
                } else {
                    if (isDetailedPermissionRequired) {
                        val detailedPermission = mutableMapOf<String, Boolean>()
                        detailedPermission[deniedPermissionForRationale] = false
                        this.callback(PermissionResult.DetailedPermissionResult(false, detailedPermission.entries))
                    } else {
                        this.callback(PermissionResult.IsPermissionGranted(isGranted = false))
                    }
                    clear()
                }
            }
            else -> {
                permissionsResult?.launch(requiredPermissions.toTypedArray())
            }
        }

    }

    /**
     * Show Rationale Dialog if user deny the permission and also if #rationale is true
     * show Neutral Button if #neutralButtonVisibility == View.VISIBLE
     * @throws IllegalArgumentException if any of the below values set to empty, as those are mandatory fields
     * */
    private fun displayRationaleDialog() {
        when {
            title.isEmpty() -> throw IllegalArgumentException("Title text should not be empty")
            description.isEmpty() -> throw IllegalArgumentException("Description text should not be empty")
            positiveText.isEmpty() -> throw IllegalArgumentException("Positive Button text should not be empty")
            negativeText.isEmpty() -> throw IllegalArgumentException("Negative Button text should not be empty")
        }

        val builder = AlertDialog.Builder(activity)
        builder.setTitle(title)
        builder.setMessage(description)
        builder.setNegativeButton(negativeText) {dialog, _ ->
            dialog.dismiss()
            callback(PermissionResult.NegativeButtonClicked)
            clear()
        }
        builder.setPositiveButton(positiveText) {dialog, _ ->
            dialog.dismiss()
            permissionsResult?.launch(requiredPermissions.toTypedArray())
        }
        if (neutralButtonVisibility == View.VISIBLE) {
            if (neutralText.isEmpty()) {
                throw IllegalArgumentException("Neutral Button text should not be empty")
            }

            builder.setNeutralButton(neutralText) { dialog, _ ->
                dialog.dismiss()
                callback(PermissionResult.NeutralButtonClicked)
                clear()
            }
        }

        builder.setCancelable(isRationaleDialogCancelable)
        builder.show()
    }

    /**
     * Navigates the app to settings
     * */
    fun notificationSettingsDialog(
        @NonNull description: String,
        @NonNull positiveText: String,
        @NonNull isCancelable: Boolean
    ) {
        when {
            description.isEmpty() -> throw IllegalArgumentException("Description text should not be empty")
            positiveText.isEmpty() -> throw IllegalArgumentException("Positive Button text should not be empty")
        }
        val builder = AlertDialog.Builder(activity)
        builder.setMessage(description)
        builder.setPositiveButton(positiveText) { dialog, _ ->
            dialog.dismiss()
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val uri: Uri = Uri.fromParts("package", activity.packageName, null)
            intent.data = uri
            activity.startActivity(intent)
        }
        builder.setCancelable(isCancelable)
        builder.show()
    }

    /**
     * Dispatches these all calls to receiver
     * */
    sealed class PermissionResult {
        data class IsPermissionGranted(val isGranted: Boolean): PermissionResult()
        data class DetailedPermissionResult(val isGranted: Boolean, val detailedPermission: Set<Map.Entry<String, Boolean>>): PermissionResult()
        object NegativeButtonClicked: PermissionResult()
        object NeutralButtonClicked: PermissionResult()
    }

    /**
     * Receives lifecycle change and uses new #Activity.registerForActivityResult() which gets the result from system permissions
     *
     * @see LifecycleEventObserver.onStateChanged
     * */
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when(event){
            Lifecycle.Event.ON_CREATE -> {
                permissionsResult = activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                    var isGranted = true
                    permissions.entries.forEach {
                        if (!it.value) {
                            isGranted = false
                            return@forEach
                        }
                    }

                    if (rationale &&
                        activity.shouldShowRequestPermissionRationale(deniedPermissionForRationale)
                    ) {
                        displayRationaleDialog()
                    } else {
                        if (isDetailedPermissionRequired) {
                            callback(PermissionResult.DetailedPermissionResult(isGranted, permissions.entries))
                            clear()
                        } else {
                            callback(PermissionResult.IsPermissionGranted(isGranted))
                            clear()
                        }
                    }
                }

            }
            Lifecycle.Event.ON_START -> {}
            Lifecycle.Event.ON_RESUME -> {}
            Lifecycle.Event.ON_PAUSE -> {}
            Lifecycle.Event.ON_STOP -> {}
            Lifecycle.Event.ON_DESTROY -> {
                permissionsResult = null
            }
            Lifecycle.Event.ON_ANY -> {}
        }
    }
}