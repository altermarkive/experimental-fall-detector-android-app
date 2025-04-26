package altermarkive.guardian

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.provider.Settings.Secure
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

object Report {
    private val TAG = Report::class.java.name

    fun probe(context: Context): JSONObject {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val list = manager.getSensorList(Sensor.TYPE_ALL)
        val types = IntArray(list.size)
        val vendors: Array<String> = Array(list.size) { "" }
        val names: Array<String> = Array(list.size) { "" }
        val resolutions = FloatArray(list.size)
        val delays = IntArray(list.size)
        val ranges = FloatArray(list.size)
        val powers = FloatArray(list.size)
        for ((i, sensor) in list.withIndex()) {
            types[i] = sensor.type
            vendors[i] = sensor.vendor
            names[i] = sensor.name
            resolutions[i] = sensor.resolution
            delays[i] = sensor.minDelay
            ranges[i] = sensor.maximumRange
            powers[i] = sensor.power
        }
        return report(context, types, vendors, names, resolutions, delays, ranges, powers)
    }

    private fun report(
        context: Context,
        types: IntArray,
        vendors: Array<String>,
        names: Array<String>,
        resolutions: FloatArray,
        delays: IntArray,
        ranges: FloatArray,
        powers: FloatArray
    ): JSONObject {
        val report = JSONObject()
        val device = reportDevice(context)
        report.put("device", device)
        val sensors = JSONArray()
        report.put("sensors", sensors)
        var i = 0
        do {
            val sensor = JSONObject()
            putInt(types, i, sensor, "type")
            putString(vendors, i, sensor, "vendor")
            putString(names, i, sensor, "name")
            putFloat(resolutions, i, sensor, "resolution")
            putInt(delays, i, sensor, "delay")
            putFloat(ranges, i, sensor, "range")
            putFloat(powers, i, sensor, "power")
            if (0 < sensor.length()) {
                sensors.put(sensor)
            }
            i++
        } while (i == sensors.length())
        return report
    }

    private fun putInt(array: IntArray, index: Int, values: JSONObject, name: String) {
        if (index < array.size) {
            values.put(name, array[index])
        }
    }

    private fun putFloat(array: FloatArray, index: Int, values: JSONObject, name: String) {
        if (index < array.size) {
            values.put(name, array[index])
        }
    }

    private fun putString(array: Array<String>, index: Int, values: JSONObject, name: String) {
        if (index < array.size) {
            values.put(name, array[index])
        }
    }

    private fun reportDevice(context: Context): JSONObject {
        val device = JSONObject()
        device.put("id", hash(id(context)))
        device.put("manufacturer", Build.MANUFACTURER)
        device.put("brand", Build.BRAND)
        device.put("product", Build.PRODUCT)
        device.put("model", Build.MODEL)
        device.put("design", Build.DEVICE)
        device.put("board", Build.BOARD)
        device.put("hardware", Build.HARDWARE)
        device.put("build", Build.FINGERPRINT)
        return device
    }

    @SuppressLint("HardwareIds")
    internal fun id(context: Context): String {
        return Secure.getString(context.contentResolver, Secure.ANDROID_ID)
    }

    private fun hash(text: String): String? {
        var hash: String? = null
        try {
            val md = MessageDigest.getInstance("SHA-256")
            md.update(text.toByteArray())
            val integer = BigInteger(1, md.digest())
            hash = String.format("%1$032X", integer)
        } catch (exception: NoSuchAlgorithmException) {
            val trace: String = android.util.Log.getStackTraceString(exception)
            val message = String.format("SHA-256 is not available:\n%s", trace)
            Log.e(TAG, message)
        }
        return hash
    }
}