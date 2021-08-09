package altermarkive.guardian

import android.annotation.SuppressLint
import android.content.Context
import android.os.PowerManager

class Sampler private constructor(private val guardian: Guardian) {
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
    }

    companion object {
        private val TAG = Sampler::class.java.name

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
}