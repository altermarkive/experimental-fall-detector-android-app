package altermarkive.guardian

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build

class Alarm private constructor(val context: Guardian) {
    private var pool: SoundPool
    private var id: Int

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val audioAttributes: AudioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()
            pool = SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(audioAttributes)
                .build()
        } else {
            @Suppress("DEPRECATION")
            pool = SoundPool(5, AudioManager.STREAM_ALARM, 0)
        }
        id = pool.load(context.applicationContext, R.raw.alarm, 1)
    }

    companion object {
        private var singleton: Alarm? = null

        internal fun instance(context: Guardian): Alarm {
            var singleton = this.singleton
            if (singleton == null) {
                singleton = Alarm(context)
                this.singleton = singleton
            }
            return singleton
        }

        internal fun siren(context: Context) {
            loudest(context, AudioManager.STREAM_ALARM)
            val singleton = this.singleton
            if (singleton != null) {
                val pool = singleton.pool
                pool.play(singleton.id, 1.0f, 1.0f, 1, 3, 1.0f)
            }
        }

        internal fun loudest(context: Context, stream: Int) {
            val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val loudest = manager.getStreamMaxVolume(stream)
            manager.setStreamVolume(stream, loudest, 0)
        }

        internal fun alert(context: Context) {
            val contact = Contact[context]
            if (contact != null && "" != contact) {
                Guardian.say(
                    context,
                    android.util.Log.WARN,
                    TAG,
                    "Alerting the emergency phone number ($contact)"
                )
                Messenger.sms(context, Contact[context], "Fall Detected")
                Telephony.call(context, contact)
            } else {
                Guardian.say(context, android.util.Log.ERROR, TAG, "ERROR: Emergency phone number not set")
                siren(context)
            }
        }

        private val TAG: String = Alarm::class.java.simpleName
    }
}