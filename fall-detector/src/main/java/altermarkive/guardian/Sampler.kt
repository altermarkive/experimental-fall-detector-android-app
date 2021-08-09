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
    //    private val config: Config
//    private val data: Data
    fun context(): Context {
        return guardian.applicationContext
    }

//    fun config(): Config {
//        return config
//    }

//    fun data(): Data {
//        return data
//    }

    @SuppressLint("WakelockTimeout")
    private fun initiate() {
        val manager = context().getSystemService(Context.POWER_SERVICE) as PowerManager
        val lock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG)
        if (!lock.isHeld) {
            lock.acquire()
        }
        val context = context()
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        if (preferences.getBoolean(context.getString(R.string.collection), false)) {
            sensors()
        }
    }

    private fun sensors() {
        val manager = context().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val list: List<Sensor> = manager.getSensorList(Sensor.TYPE_ALL)
        for (sensor in list) {
            manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST)
        }
    }

    companion object {
        private val TAG = Sampler::class.java.name
        private val MIN_TYPE: Int = 1
        private val MAX_TYPE: Int = 21

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
//        config = Config(this)
//        data = Data()
        initiate()
    }

    override fun onSensorChanged(event: SensorEvent) {
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        Log.i(TAG, "Sensor type ${sensor.type} (${sensor.name}, ${sensor.vendor}) changed accuracy to: $accuracy")
    }
}