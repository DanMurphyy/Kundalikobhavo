package com.hfad.kundalikob_havo

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.hfad.kundalikob_havo.databinding.ActivityMainBinding
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener

class MainActivity : AppCompatActivity() {
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    private var mProgressDialog: Dialog? = null
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    private var mLatitude: Double = 0.0
    private var mLongitude: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        locationInfo()

        setContentView(binding.root)
    }

    private fun locationInfo() {
        if (!isLocationEnabled()) {
            showLocationDialogForPermissions()
        } else {
            val dexter = Dexter.withActivity(this)
            dexter.withPermissions(Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        if (report!!.areAllPermissionsGranted()) {
                            requestLocationData()
                        }
                        if (report.isAnyPermissionPermanentlyDenied) {
                            Toast.makeText(this@MainActivity,
                                "Siz Joylashuv aniqlovi ruxsatlarini rad etdingiz, Iltimos, ularni yoqing, chunki ilova ishlashi uchun bu zarur.",
                                Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?,
                    ) {
                        showRationalDialogForPermissions()
                    }

                })

        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationData() {
        val mLocationRequest = com.google.android.gms.location.LocationRequest()
        mLocationRequest.priority =
            com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY

        mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallBack,
            Looper.myLooper())
    }

    private val mLocationCallBack = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location = locationResult.lastLocation
            mLatitude = mLastLocation.latitude
            mLongitude = mLastLocation.longitude

            getLocationWeatherDetails()
        }
    }

    private fun getLocationWeatherDetails() {
        TODO("Not yet implemented")
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        return isGpsEnabled || isNetworkEnabled
    }

    private fun showRationalDialogForPermissions() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Ilovaga kerakli bo'lgan ruhsatlar berilmagan, Iltimos bularni Qurilma sozlamalariga kirib yoqib qoying")
        builder.setPositiveButton("Sozlamalarga o'tish") { _, _ ->
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                e.printStackTrace()
            }
        }
        builder.setNegativeButton("bekor qilish") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun showLocationDialogForPermissions() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Sizni Qurilmangizni joylashuv aniqlovi ochirilgan.Iltimos yoqib qoying")
        builder.setPositiveButton("Sozlamalarga o'tish") { _, _ ->
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }
        builder.setNegativeButton("bekor qilish") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun showCustomProgressDialog() {
        mProgressDialog = Dialog(this)
        mProgressDialog!!.setContentView(R.layout.dialog_custom_progress)
        mProgressDialog!!.window!!.setBackgroundDrawableResource(com.karumi.dexter.R.color.mtrl_btn_transparent_bg_color)
        mProgressDialog!!.show()
    }

    private fun hideProgressDialog() {
        mProgressDialog!!.dismiss()
    }

}