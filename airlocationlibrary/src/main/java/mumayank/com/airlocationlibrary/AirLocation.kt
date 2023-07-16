package mumayank.com.airlocationlibrary

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.location.Location
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.google.android.gms.location.*
import mumayank.com.airlocationlibrary.helpers.*
import mumayank.com.airpermissions.AirPermissions
import java.io.Serializable
import java.lang.ref.WeakReference

@SuppressLint("MissingPermission")
class AirLocation(
    activity: Activity,
    private val callback: Callback?,
//    private val isLocationRequiredOnlyOneTime: Boolean = false,
    private val locationInterval: Long = 0,
    private val toastTextWhenOpenAppSettingsIfPermissionsPermanentlyDenied: String = "Please enable location permissions from settings to proceed"
) : Serializable {
    /*
    declarations
     */
    private lateinit var locationCallback: LocationCallback
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var isStartCalled = false

    private val activityWeakReference = WeakReference(activity)

    private val googlePlayApiHelper = GooglePlayApiHelper(activity, fun() {
        if (activityWeakReference.get() == null) {
            onFailure(LocationFailedEnum.ACTIVITY_NOT_FOUND)
            return
        }

        getLocationPermissions()
    }, fun() {
        if (activityWeakReference.get() == null) {
            onFailure(LocationFailedEnum.ACTIVITY_NOT_FOUND)
            return
        }

        onFailure(LocationFailedEnum.GOOGLE_PLAY_API_NOT_AVAILABLE)
    })

    private val airPermissions = AirPermissions(activity, arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ), fun() {
        checkIfInFlightMode()
    }, fun() {
        onFailure(LocationFailedEnum.LOCATION_PERMISSION_NOT_GRANTED)
    })

    private val locationOptimizationPermissionHelper = LocationOptimizationPermissionHelper(
        activity,
        locationInterval,
        true,
        fun() {
            if (activityWeakReference.get() == null) {
                return
            }

            getFusedLocation()
        }, fun(locationFailedEnum: LocationFailedEnum) {
            if (activityWeakReference.get() == null) {
                return
            }

            onFailure(locationFailedEnum)
        }
    )

    enum class LocationFailedEnum {
        GOOGLE_PLAY_API_NOT_AVAILABLE,
        DEVICE_IN_FLIGHT_MODE,
        LOCATION_PERMISSION_NOT_GRANTED,
        LOCATION_OPTIMIZATION_PERMISSION_NOT_GRANTED,
        COULD_NOT_OPTIMIZE_DEVICE_HARDWARE,
        HIGH_PRECISION_LOCATION_NA_TRY_AGAIN_PREFERABLY_WITH_NETWORK_CONNECTIVITY,
        ACTIVITY_NOT_FOUND,
        TIMEOUT
    }

    interface Callback {
        fun onSuccess(locations: ArrayList<Location>)
        fun onFailure(locationFailedEnum: LocationFailedEnum)
    }

    private fun onFailure(locationFailedEnum: LocationFailedEnum) {
        timeoutHandler.removeCallbacks(timeoutRunnable)
        callback?.onFailure(locationFailedEnum)
    }

    /*
    start of logic
     */
    fun start(timeout: Long = 30000) {
        if (activityWeakReference.get() == null) {
            onFailure(LocationFailedEnum.ACTIVITY_NOT_FOUND)
            return
        }

        this.timeout = timeout

        isStartCalled = true
        makeGooglePlayApiAvailable()
    }

    private fun makeGooglePlayApiAvailable() {
        if (activityWeakReference.get() == null) {
            onFailure(LocationFailedEnum.ACTIVITY_NOT_FOUND)
            return
        }

        googlePlayApiHelper.makeItAvailable()
    }

    private fun getLocationPermissions() {
        if (activityWeakReference.get() == null) {
            onFailure(LocationFailedEnum.ACTIVITY_NOT_FOUND)
            return
        }

        airPermissions.request()
    }

    private fun checkIfInFlightMode() {
        if (activityWeakReference.get() == null) {
            onFailure(LocationFailedEnum.ACTIVITY_NOT_FOUND)
            return
        }

        if (NetworkHelper.isInFlightMode(activityWeakReference.get() as Activity)) {
            onFailure(LocationFailedEnum.DEVICE_IN_FLIGHT_MODE)
        } else {
            getOptimizationPermissions()
        }
    }

    private fun getOptimizationPermissions() {
        if (activityWeakReference.get() == null) {
            onFailure(LocationFailedEnum.ACTIVITY_NOT_FOUND)
            return
        }

        locationOptimizationPermissionHelper.getPermission()
    }

    private fun getFusedLocation() {
        if (activityWeakReference.get() == null) {
            onFailure(LocationFailedEnum.ACTIVITY_NOT_FOUND)
            return
        }
        val activityTemp = activityWeakReference.get() ?: return

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activityTemp)
        requestLocationUpdates()
//        addLifecycleListener()

//        val task = fusedLocationClient.lastLocation
//        task.addOnSuccessListener { location: Location? ->
//            if (activityWeakReference.get() == null) {
//                return@addOnSuccessListener
//            }
//
//            if (location != null) {
////                callback?.onSuccess(arrayListOf(location))
//                if (isLocationRequiredOnlyOneTime.not()) {
//                    addLifecycleListener()
//                }
//            } else {
//                addLifecycleListener()
//            }
//        }.addOnFailureListener {
//            if (activityWeakReference.get() == null) {
//                return@addOnFailureListener
//            }
//
//            addLifecycleListener()
//        }
    }

    private fun addLifecycleListener() {
        if (activityWeakReference.get() == null) {
            onFailure(LocationFailedEnum.ACTIVITY_NOT_FOUND)
            return
        }
        val activityTemp = activityWeakReference.get() ?: return

        (activityTemp as LifecycleOwner).lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
            fun connectListener() {
                if (activityWeakReference.get() == null) {
                    onFailure(LocationFailedEnum.ACTIVITY_NOT_FOUND)
                    return
                }

                requestLocationUpdates()
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            fun disconnectListener() {
                if (activityWeakReference.get() == null) {
                    onFailure(LocationFailedEnum.ACTIVITY_NOT_FOUND)
                    return
                }

                fusedLocationClient.removeLocationUpdates(locationCallback)
            }
        })
    }

    private fun requestLocationUpdates() {
        if (activityWeakReference.get() == null) {
            onFailure(LocationFailedEnum.ACTIVITY_NOT_FOUND)
            return
        }
        val replySubmitted = booleanArrayOf(false)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                // Cancel the timeout mechanism since we received a location update
                timeoutHandler.removeCallbacks(timeoutRunnable)

                if (activityWeakReference.get() == null) {
                    onFailure(LocationFailedEnum.ACTIVITY_NOT_FOUND)
                    return
                }
                if (replySubmitted[0]) {
                    return
                }
                replySubmitted[0] = true
                fusedLocationClient.removeLocationUpdates(locationCallback)
                callback?.onSuccess(locationResult.locations as ArrayList<Location>)
            }

            override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                if (activityWeakReference.get() == null) {
                    onFailure(LocationFailedEnum.ACTIVITY_NOT_FOUND)
                    return
                }
                if (replySubmitted[0]) {
                    return
                }
                replySubmitted[0] = true
                fusedLocationClient.removeLocationUpdates(locationCallback)

                if (!locationAvailability.isLocationAvailable) {
                    onFailure(LocationFailedEnum.HIGH_PRECISION_LOCATION_NA_TRY_AGAIN_PREFERABLY_WITH_NETWORK_CONNECTIVITY)
//                    fusedLocationClient.removeLocationUpdates(locationCallback)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            LocationOptimizationPermissionHelper.getLocationRequest(
                locationInterval,
                true
            ),
            locationCallback,
            Looper.getMainLooper()
        )
        timeoutHandler.postDelayed(timeoutRunnable, timeout)

    }

    private var timeout: Long = 30000

    private val timeoutHandler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        callback?.onFailure(LocationFailedEnum.TIMEOUT)
    }


    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (activityWeakReference.get() == null) {
            return
        }

        if (isStartCalled.not()) {
            return
        }

        airPermissions.onRequestPermissionsResult(requestCode)
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (activityWeakReference.get() == null) {
            return
        }

        if (isStartCalled.not()) {
            return
        }

        airPermissions.onActivityResult(requestCode)
        locationOptimizationPermissionHelper.onActivityResult(requestCode, resultCode, data)
        googlePlayApiHelper.onActivityResult(requestCode)
    }

}