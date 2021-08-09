package altermarkive.uploader

import altermarkive.uploader.Report
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.provider.Settings.Secure
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

object Report {
    private val TAG = Report::class.java.name
    @Throws(JSONException::class)
    fun probe(context: Context): String {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val list = manager.getSensorList(Sensor.TYPE_ALL)
        val types = IntArray(list.size)
        val vendors = arrayOfNulls<String>(list.size)
        val names = arrayOfNulls<String>(list.size)
        val resolutions = FloatArray(list.size)
        val delays = IntArray(list.size)
        val ranges = FloatArray(list.size)
        val powers = FloatArray(list.size)
        var i = 0
        for (sensor in list) {
            types[i] = sensor.type
            vendors[i] = sensor.vendor
            names[i] = sensor.name
            resolutions[i] = sensor.resolution
            delays[i] = sensor.minDelay
            ranges[i] = sensor.maximumRange
            powers[i] = sensor.power
            i++
        }
        return report(context, types, vendors, names, resolutions, delays, ranges, powers)
    }

    @Throws(JSONException::class)
    fun report(
        context: Context,
        types: IntArray?,
        vendors: Array<String?>?,
        names: Array<String?>?,
        resolutions: FloatArray?,
        delays: IntArray?,
        ranges: FloatArray?,
        powers: FloatArray?
    ): String {
        val report = JSONObject()
        val device = reportDevice(context)
        report.put("device", device)
        val sensors = JSONArray()
        device.put("sensors", sensors)
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
        return report.toString()
    }

    @Throws(JSONException::class)
    private fun putInt(array: IntArray?, index: Int, values: JSONObject, name: String) {
        if (array != null && index < array.size) {
            values.put(name, array[index])
        }
    }

    @Throws(JSONException::class)
    private fun putFloat(array: FloatArray?, index: Int, values: JSONObject, name: String) {
        if (array != null && index < array.size) {
            values.put(name, reportFloat(array[index]))
        }
    }

    @Throws(JSONException::class)
    private fun putString(array: Array<String?>?, index: Int, values: JSONObject, name: String) {
        if (array != null && index < array.size) {
            values.put(name, array[index])
        }
    }

    @Throws(JSONException::class)
    private fun reportFloat(value: Float): JSONObject {
        val hifi = JSONObject()
        var bits = java.lang.Float.floatToRawIntBits(value).toLong()
        hifi.put("sign", bits shr 31)
        bits = bits and 0x7FFFFFFF
        hifi.put("exponent", bits shr 23)
        bits = bits and 0x007FFFFF
        hifi.put("mantissa", bits)
        return hifi
    }

    @Throws(JSONException::class)
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

    fun id(context: Context): String {
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
            val trace: String = Log.getStackTraceString(exception)
            val message = String.format("MD5 is not available:\n%s", trace)
            Log.e(TAG, message)
        }
        return hash
    }
}