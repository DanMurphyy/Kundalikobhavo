package com.hfad.kundalikob_havo

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.hfad.kundalikob_havo.databinding.ActivityMainBinding
import com.hfad.kundalikob_havo.models.WeatherResponse
import com.hfad.kundalikob_havo.network.Constants
import com.hfad.kundalikob_havo.network.WeatherService
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    private var mProgressDialog: Dialog? = null
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    private var mLatitude: Double = 0.0
    private var mLongitude: Double = 0.0

    private lateinit var mSharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        mSharedPreferences = getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE)
        setUpUI()
        locationInfo()

        binding.refresh.setOnClickListener {
            locationInfo()
            getLocationWeatherDetails()
        }

        setContentView(binding.root)
    }

    private fun locationInfo() {
        if (!isLocationEnabled()) {
            showLocationDialogForPermissions()
        } else {
            Dexter.withActivity(this)
                .withPermissions(Manifest.permission.ACCESS_FINE_LOCATION,
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

                }).onSameThread()
                .check()

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
        if (Constants.isNetworkAvailable(this)) {
            val retrofit: Retrofit = Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service: WeatherService = retrofit.create(WeatherService::class.java)

            val listCall: Call<WeatherResponse> = service.getWeather(
                mLatitude, mLongitude, Constants.METRIC_UNIT, Constants.APP_ID, Constants.Lang
            )
            showCustomProgressDialog()
            listCall.enqueue(object : Callback<WeatherResponse> {
                override fun onResponse(
                    call: Call<WeatherResponse>,
                    response: Response<WeatherResponse>,
                ) {
                    hideProgressDialog()
                    if (response.isSuccessful) {
                        val weatherList: WeatherResponse? = response.body()
                        val weatherResponseJsonString = Gson().toJson(weatherList)
                        val editor = mSharedPreferences.edit()
                        editor.putString(Constants.WEATHER_RESPONSE_DATA, weatherResponseJsonString)
                        editor.apply()
                        setUpUI()
                    } else {
                        when (response.code()) {
                            400 -> {
                                Log.e("Error 400", "Bad connection")
                            }
                            404 -> {
                                Log.e("Error 404", "Not found")
                            }
                            else -> {
                                Log.e("Error", "Generic Error")
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    Log.e("Errorrr", t.message.toString())
                    hideProgressDialog()
                }
            })
        } else {
            Toast.makeText(this,
                "Internet ulanmagan, Iltimos tekshirib ko'rin",
                Toast.LENGTH_SHORT).show()
        }
    }

    private fun setUpUI() {
        val weatherResponseJsonString =
            mSharedPreferences.getString(Constants.WEATHER_RESPONSE_DATA, "")
        if (!weatherResponseJsonString.isNullOrEmpty()) {
            val weatherList =
                Gson().fromJson(weatherResponseJsonString, WeatherResponse::class.java)
            for (item in weatherList.weather.indices) {
                binding.tvMain.text = weatherList.weather[item].main
                binding.tvDescription.text = weatherList.weather[item].description
                binding.tvTemp.text = weatherList.main.temp.toString() + getUnit()

                binding.tvHumidity.text = weatherList.main.humidity.toString() + "%"
                binding.tvMin.text = weatherList.main.feels_like.toString() + getUnit()
                val mileToKM = weatherList.wind.speed * 1.60934
                val kilo = BigDecimal(mileToKM).setScale(2, RoundingMode.HALF_EVEN).toString()
                binding.tvSpeed.text = kilo

                binding.tvName.text =
                    if (weatherList.name == "Asaka" || weatherList.name == "Асака") "Shahrixon" else weatherList.name
                binding.tvCountry.text = weatherList.sys.country

                binding.tvSunrise.text = unixTime(weatherList.sys.sunrise)
                binding.tvSunset.text = unixTime(weatherList.sys.sunset)

                when (weatherList.weather[item].icon) {
                    "01d" -> binding.ivMain.setImageResource(R.drawable.sun)
                    "02d" -> binding.ivMain.setImageResource(R.drawable.cloud)
                    "03d" -> binding.ivMain.setImageResource(R.drawable.cloud)
                    "04d" -> binding.ivMain.setImageResource(R.drawable.cloud)
                    "04n" -> binding.ivMain.setImageResource(R.drawable.cloud)
                    "10d" -> binding.ivMain.setImageResource(R.drawable.rain)
                    "11d" -> binding.ivMain.setImageResource(R.drawable.storm)
                    "13d" -> binding.ivMain.setImageResource(R.drawable.snowflake)
                    "01n" -> binding.ivMain.setImageResource(R.drawable.cloud)
                    "02n" -> binding.ivMain.setImageResource(R.drawable.cloud)
                    "03n" -> binding.ivMain.setImageResource(R.drawable.cloud)
                    "10n" -> binding.ivMain.setImageResource(R.drawable.cloud)
                    "11n" -> binding.ivMain.setImageResource(R.drawable.rain)
                    "13n" -> binding.ivMain.setImageResource(R.drawable.snowflake)
                    "50d" -> binding.ivMain.setImageResource(R.drawable.fog)
                    "50n" -> binding.ivMain.setImageResource(R.drawable.fog)
                }
            }
        }
    }

    private fun getUnit(): String? {
        return if ("US" == resources.configuration.locale.country ||
            "LR" == resources.configuration.locale.country ||
            "MM" == resources.configuration.locale.country
        ) {
            "°F"
        } else {
            "°C"
        }
    }

    private fun unixTime(timex: Long): String {
        val date = Date(timex * 1000)
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.timeZone = TimeZone.getDefault()

        return sdf.format(date)
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