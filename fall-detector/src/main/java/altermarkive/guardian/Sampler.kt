package altermarkive.uploader

import android.content.Context
import android.os.PowerManager

class Sampler private constructor(private val context: Context) {
    private val config: Config
    private val data: Data
    fun context(): Context {
        return context
    }

    fun config(): Config {
        return config
    }

    fun data(): Data {
        return data
    }

    fun initiate(context: Context) {
        val manager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val lock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG)
        if (!lock.isHeld) {
            lock.acquire()
        }
        initiate()
    }

    private external fun initiate()

    companion object {
        private val TAG = Sampler::class.java.name

        @Volatile
        private var instance: Sampler? = null
        @Synchronized
        fun instance(context: Context): Sampler? {
            if (instance == null) {
                instance = Sampler(context)
            }
            return instance
        }

        init {
            System.loadLibrary("sampler")
        }
    }

    init {
        config = Config(this)
        data = Data()
    }
}