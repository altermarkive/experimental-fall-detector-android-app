package altermarkive.guardian

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import android.view.KeyEvent
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.lang.reflect.Method


// See also: https://stackoverflow.com/questions/51871673/how-to-answer-automatically-and-programatically-an-incoming-call-on-android

class Telephony : BroadcastReceiver() {
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {
        val manager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        when (manager.callState) {
            TelephonyManager.CALL_STATE_RINGING -> {
                @Suppress("DEPRECATION")
                val contact = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                Guardian.say(context, android.util.Log.WARN, TAG, "Receiving call from ${contact.toString()}")
                if (Contact.check(context, contact)) {
                    Guardian.say(context, android.util.Log.WARN, TAG, "Answering the call")
                    answer(context)
                }
                return
            }
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                speakerphone(context, true)
                return
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                speakerphone(context, false)
                return
            }
        }
    }

    companion object {
        internal fun speakerphone(context: Context, on: Boolean) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (on) {
                audioManager.mode = AudioManager.MODE_IN_CALL
                Alarm.loudest(context, AudioManager.STREAM_VOICE_CALL)
            }
            audioManager.isSpeakerphoneOn = on
        }

        private fun answer(context: Context) {
            try {
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                        val telecomManager =
                            context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                        if (ActivityCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ANSWER_PHONE_CALLS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            Guardian.say(
                                context,
                                android.util.Log.ERROR,
                                TAG,
                                "ERROR: No permission to accept calls"
                            )
                            return
                        }
                        @Suppress("DEPRECATION")
                        telecomManager.acceptRingingCall()
                    }
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> {
                        throughMediaController(context)
                    }
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> {
                        throughAudioManager(context)
                    }
                }
            } catch (exception: Exception) {
                throughReceiver(context)
            }
        }

        private fun getTelephonyService(context: Context): Any? {
            val telephonyManager =
                context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            try {
                val method: Method = Class.forName(telephonyManager.javaClass.name)
                    .getDeclaredMethod("getITelephony")
                method.isAccessible = true
                return method.invoke(telephonyManager)
            } catch (exception: Exception) {
                Log.e(TAG, exception.stackTraceToString())
            }
            return null
        }

        private fun throughTelephonyService(context: Context) {
            val telephonyService = getTelephonyService(context)
            if (telephonyService != null) {
                val silenceRinger = telephonyService.javaClass.getDeclaredMethod("silenceRinger")
                silenceRinger.invoke(telephonyService)
                val answerRingingCall =
                    telephonyService.javaClass.getDeclaredMethod("answerRingingCall")
                answerRingingCall.invoke(telephonyService)
            }
        }

        @RequiresApi(Build.VERSION_CODES.KITKAT)
        private fun throughAudioManager(context: Context) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HEADSETHOOK)
            val upEvent = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HEADSETHOOK)
            audioManager.dispatchMediaKeyEvent(downEvent)
            audioManager.dispatchMediaKeyEvent(upEvent)
        }

        private fun throughReceiver(context: Context) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            try {
                throughTelephonyService(context)
            } catch (exception: Exception) {
                @Suppress("DEPRECATION")
                val broadcastConnected = ("HTC".equals(Build.MANUFACTURER, ignoreCase = true)
                        && !audioManager.isWiredHeadsetOn)
                if (broadcastConnected) {
                    broadcastHeadsetConnected(context)
                }
                try {
                    Runtime.getRuntime().exec("input keyevent " + KeyEvent.KEYCODE_HEADSETHOOK)
                } catch (exception: IOException) {
                    throughPhoneHeadsetHook(context)
                } finally {
                    if (broadcastConnected) {
                        broadcastHeadsetConnected(context)
                    }
                }
            }
        }

        private fun broadcastHeadsetConnected(context: Context) {
            val intent = Intent(Intent.ACTION_HEADSET_PLUG)
            intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY)
            intent.putExtra("state", 0)  // Would be 1 if not connected
            intent.putExtra("name", "mysms")
            try {
                context.sendOrderedBroadcast(intent, null)
            } catch (ignored: Exception) {
            }
        }

        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        private fun throughMediaController(context: Context) {
            val mediaSessionManager =
                context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            try {
                val controllers: List<MediaController> = mediaSessionManager.getActiveSessions(
                    ComponentName(
                        context,
                        NotificationListenerService::class.java
                    )
                )
                for (controller in controllers) {
                    if ("com.android.server.telecom" == controller.packageName) {
                        controller.dispatchMediaButtonEvent(
                            KeyEvent(
                                KeyEvent.ACTION_UP,
                                KeyEvent.KEYCODE_HEADSETHOOK
                            )
                        )
                        break
                    }
                }
            } catch (exception: Exception) {
                throughAudioManager(context)
            }
        }

        private fun throughPhoneHeadsetHook(context: Context) {
            val buttonDown = Intent(Intent.ACTION_MEDIA_BUTTON)
            buttonDown.putExtra(
                Intent.EXTRA_KEY_EVENT,
                KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HEADSETHOOK)
            )
            context.sendOrderedBroadcast(buttonDown, "android.permission.CALL_PRIVILEGED")
            val buttonUp = Intent(Intent.ACTION_MEDIA_BUTTON)
            buttonUp.putExtra(
                Intent.EXTRA_KEY_EVENT,
                KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HEADSETHOOK)
            )
            context.sendOrderedBroadcast(buttonUp, "android.permission.CALL_PRIVILEGED")
        }

        internal fun call(context: Context, number: String) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                callNewer(context, number)
            } else {
                val applicationContext = context.applicationContext
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CALL_PHONE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Guardian.say(applicationContext, android.util.Log.ERROR, TAG, "ERROR: Not permitted to call")
                } else {
                    val call = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))
                    call.addFlags(FLAG_ACTIVITY_NEW_TASK)
                    context.applicationContext.startActivity(call)
                    speakerphone(applicationContext, true)
                }
            }
        }

        @RequiresApi(Build.VERSION_CODES.M)
        private fun callNewer(context: Context, number: String) {
            val applicationContext = context.applicationContext
            val uri = Uri.fromParts("tel", number, null)
            val extras = Bundle()
            extras.putBoolean(TelecomManager.EXTRA_START_CALL_WITH_SPEAKERPHONE, true)
            val telecomManager =
                context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CALL_PHONE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Guardian.say(applicationContext, android.util.Log.ERROR, TAG, "ERROR: Not permitted to call")
            } else {
                telecomManager.placeCall(uri, extras)
            }
        }

        private val TAG = Telephony::class.java.simpleName
    }
}