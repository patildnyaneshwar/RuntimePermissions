# Runtime System Permissions | Android M(>= API 23)

Simple SDK/Class to approach the workflow for requesting Runtime System Permission.

From Android Marshmallow (API >= 23), Google has introduced a runtime permission model. As a developer, implementing permissions in all the Activities/Fragments is a bit redundant. So, I have made a simple SDK that makes it easier to implement.

## Summary
I've created the ```RuntimePermission``` class which required ```FragmentActivity(Base class for activities)``` to be passed in the constructor. It also inherits the ```LifecycleEventObserver``` to observe the lifecycle change and dispatch the result to the receiver.

Here, We will be using ```new ActivityResultLauncher API``` while asking for runtime permissions as ```onActivityResult API``` is deprecated, and because it doesn’t require request code, it's also good at result callback.

## How to Implement
First of all, we need to initialize the RuntimePermission class in ```Activity``` or ```Fragment``` and add a lifecycle observer.

In _Activity_:
```js
val runtimePermission = RuntimePermission(this)
lifecycle.addObserver(runtimePermission)
```

In _Fragment_:
```js
val runtimePermission = RuntimePermission(requireActivity())
lifecycle.addObserver(runtimePermission)
```

and in the onDestroy() method it's required to remove the lifecycle observer
```
lifecycle.removeObserver(runtimePermission)
```

## Request Permissions
The example below shows how to request permissions for the method.
There are a few things to note:

- To request permission use ```RuntimePermission#requestPermissions(...)``` which accepts any number of permissions.
- The ```RuntimePermission#showRationalePermission(...)``` accepts the boolean, title and description. If we set boolean true only then ```Rational Dialog``` will be shown
- In case we set showRationalePermission boolean to true (Defalut is false) ```RuntimePermission#setPositiveButton(...)```, ```RuntimePermission#setNegativeButton(...)``` (mandatory) and ```RuntimePermission#setNeutralButton(...)```, ```RuntimePermission#setRationaleDialogCancelable(...) (Defalut is true)``` (Optional)
- In-case receiver require detailed permission then use ```RuntimePermission#setDetailedPermissionRequired(...)``` which returns detailed set receiver as requested (Default is false)
- Finally, ```RuntimePermission#result{...}``` callback has ```DetailedPermissionResult(...)```, ```IsPermissionGranted(...)```, ```NegativeButtonClicked```, ```NeutralButtonClicked```. Incase ```IsPermissionGranted(false)``` show the ```RuntimePermission#settingsDialog(...)``` onClick of ```RuntimePermission#settingsDialog#PositiveButton``` naviagtes to application settings, where user has to maunlly provide the access.

## Examples
### _Single permission request_
Here in permission request ```RuntimePermission#setDetailedPermissionRequired(...)``` not at all called (Default false). So, ```RuntimePermission#PermissionResult#IsPermissionGranted(...)``` will be triggered which will return a boolean value that tells whether all permissions are granted or not.

And, ```RuntimePermission#showRationalePermission(...)``` set to true. If the user "deny" permission ```Rationale Dialog``` will be triggered where the user will act accordingly. In case the user select "Never Ask Again" ```RuntimePermission#PermissionResult#IsPermissionGranted(...)``` will always be false and the application has to act accordingly.

*Note:* Entered text shouldn't be empty otherwise the application will ```throw IllegalArgumentException(...)```

```js
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
                .setNeutralButton("Maybe later")
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
```

### _Multiple permission request_
Here in permission request ```RuntimePermission#setDetailedPermissionRequired(...)``` set to true. So, ```RuntimePermission#PermissionResult#DetailedPermissionResult(...)``` will be triggered which will return detailed permission, a receiver as requested. Where the developer has to handle how the application should act accordingly.

And, ```RuntimePermission#showRationalePermission(...)``` method is not called. Therefore ```Rationale dialog``` will not be triggered in case the user "denies" permission, instead "isGranted = false" in the callback.

```js
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
```

### _Incomplete permission request_
As you know from Android 11 (API >= 30) ```WRITE_EXTERNAL_STORAGE``` and ```READ_EXTERNAL_STORAGE```were removed, instead, we need to use ```MANAGE_EXTERNAL_STORAGE```(permission prompt dialog will not show, instead navigate to settings) to access storage. So, set ```RuntimePermission#setDetailedPermissionRequired(...)```  to true and act accordingly.

```js
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
                .result { result ->
                    when (result) {
                        is RuntimePermission.PermissionResult.DetailedPermissionResult -> {
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
                                    Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
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
```

I hope this SDK will be helpful for you. Feel free to give your feedback. I would be very happy to get new suggestions and modifications in the code.

Happy coding :)

## Reference

```
https://github.com/googlesamples/easypermissions
```
