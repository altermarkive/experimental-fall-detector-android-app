package altermarkive.guardian

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import android.util.Log
import androidx.preference.PreferenceManager

class Sampler private constructor(private val guardian: Guardian) : SensorEventListener {
    private val data: Data = Data(guardian)

    fun context(): Context {
        return guardian.applicationContext
    }

//    fun data(): Data {
//        return data
//    }

    @SuppressLint("WakelockTimeout")
    private fun initiate() {
        val context = context()
        val manager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val lock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG)
        if (!lock.isHeld) {
            lock.acquire()
        }
        probe(context)
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        if (preferences.getBoolean(context.getString(R.string.collection), false)) {
            sensors()
        }
    }

    private fun probe(context: Context) {
        val report = Report.probe(context)
        val device = report.getJSONObject("device")
        Log.i(TAG, "Device: $device")
        val sensors = report.getJSONArray("sensors")
        for (i in 0 until sensors.length()) {
            Log.i(TAG, "Sensor: ${sensors.get(i)}")
        }
    }

    private fun sensors() {
        val manager = context().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val list: List<Sensor> = manager.getSensorList(Sensor.TYPE_ALL)
        for (sensor in list) {
            if (sensor.type in MIN_TYPE..MAX_TYPE) {
                manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST)
            }
        }
    }

    companion object {
        private val TAG = Sampler::class.java.name
        private const val MIN_TYPE: Int = 1
        private const val MAX_TYPE: Int = 13

        @Volatile
        private var instance: Sampler? = null

        @Synchronized
        fun instance(guardian: Guardian): Sampler {
            var instance = this.instance
            if (instance == null) {
                instance = Sampler(guardian)
                this.instance = instance
            }
            return instance
        }
    }

    init {
        initiate()
    }

    override fun onSensorChanged(event: SensorEvent) {
        data.dispatch(event.sensor.type, event.timestamp, event.values)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        Log.i(
            TAG,
            "Sensor type ${sensor.type} (${sensor.name}, ${sensor.vendor}) changed accuracy to: $accuracy"
        )
    }
}