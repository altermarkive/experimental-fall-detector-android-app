package altermarkive.guardian

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.provider.Settings
//import android.provider.Settings.Secure
import androidx.core.app.ActivityCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Positioning private constructor(private val context: Guardian) : LocationListener {
    private var gps: Location? = null
    private var network: Location? = null
    private var once: Long = 0
    private var replied: Boolean = true
    private val format: SimpleDateFormat = SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.US)

    internal fun trigger() {
        enforce(context)
        synchronized(this) {
            reset(0, 0f)
            once = System.currentTimeMillis()
            replied = false
        }
    }

    private fun reset(minTimeMs: Long, minDistanceM: Float) {
        val applicationContext: Context = context.applicationContext
        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val manager =
            applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (gps == null) {
            gps = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        }
        if (network == null) {
            network = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        }
        manager.removeUpdates(this)
        manager.requestLocationUpdates(
            LocationManager.NETWORK_PROVIDER,
            minTimeMs,
            minDistanceM,
            this
        )
    }

    private fun accuracy(location: Location?): Float {
        return if (null != location && location.hasAccuracy()) {
            location.accuracy
        } else {
            Float.POSITIVE_INFINITY
        }
    }

    override fun onLocationChanged(newLocation: Location) {
        var location: Location = newLocation
        enforce(context)
        synchronized(this) {
            if (LocationManager.GPS_PROVIDER == location.provider) {
                if (accuracy(location) < accuracy(gps)) {
                    gps = location
                }
            }
            if (LocationManager.NETWORK_PROVIDER == location.provider) {
                if (accuracy(location) < accuracy(network)) {
                    network = location
                }
            }
            val deadline = once + 120000
            val now = System.currentTimeMillis()
            if (deadline <= now && !replied) {
                val battery = Battery.level(context)
                val message: String
                if (accuracy(gps).isInfinite() && accuracy(network).isInfinite()) {
                    message = "Battery: ${battery}% Location: unknown"
                } else {
                    val replacement: Location? = if (accuracy(gps) < accuracy(network)) {
                        gps
                    } else {
                        network
                    }
                    if (replacement != null) {
                        location = replacement
                    }
                    val lat: String = "%.5f".format(location.latitude)
                    val lon: String = "%.5f".format(location.longitude)
                    val accuracy: Int = location.accuracy.toInt()
                    val altitude: Int = location.altitude.toInt()
                    val bearing: Int = location.bearing.toInt()
                    val speed: Int = (location.speed * 60.0 * 60.0 / 1000.0).toInt()
                    val time: String = format.format(Date(location.time))
                    message =
                        "Battery: $battery % Location ($time): $lat,$lon ~$accuracy m ^$altitude m $bearing deg $speed km/h http://maps.google.com/?q=${lat},${lon}"
                }
                Messenger.sms(context, Contact[context], message)
                reset(METERS_10, MINUTES_10)
                replied = true
            }
        }
    }

    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
        enforce(context)
    }

    override fun onProviderEnabled(provider: String) {
        enforce(context)
    }

    override fun onProviderDisabled(provider: String) {
        enforce(context)
    }

    companion object {
        private const val METERS_10: Long = 10
        private const val MINUTES_10: Float = 600000f

        internal var singleton: Positioning? = null

        fun initiate(guardian: Guardian) {
            if (null == singleton) {
                singleton = Positioning(guardian)
            }
        }

        private fun enforce(context: Context) {
            enforceWiFi(context)
            enforceGPS(context)
        }

        private fun enforceWiFi(context: Context) {
            val applicationContext: Context = context.applicationContext
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.CHANGE_WIFI_STATE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            val wifi =
                applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            wifi.isWifiEnabled = true
        }

        private fun enforceGPS(context: Context) {
            val applicationContext: Context = context.applicationContext
            val manager =
                applicationContext.applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                return
            }
            var stealth = false
            try {
                val packages = applicationContext.packageManager
                val info =
                    packages.getPackageInfo("com.android.settings", PackageManager.GET_RECEIVERS)
                if (info != null) {
                    for (receiver in info.receivers) {
                        if (receiver.name == "com.android.settings.widget.SettingsAppWidgetProvider" && receiver.exported) {
                            stealth = true
                        }
                    }
                }
            } catch (ignored: PackageManager.NameNotFoundException) {
            }
            if (stealth) {
//                @Suppress("DEPRECATION")
//                val provider =
//                    Secure.getString(
//                        applicationContext.contentResolver,
//                        Secure.LOCATION_PROVIDERS_ALLOWED
//                    )
//                if (!provider.contains("gps")) {
                val poke = Intent()
                poke.setClassName(
                    "com.android.settings",
                    "com.android.settings.widget.SettingsAppWidgetProvider"
                )
                poke.addCategory(Intent.CATEGORY_ALTERNATIVE)
                poke.data = Uri.parse("3")
                applicationContext.sendBroadcast(poke)
//                }
            } else {
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                applicationContext.startActivity(intent)
            }
        }
    }

    init {
        reset(METERS_10, MINUTES_10)
    }
}