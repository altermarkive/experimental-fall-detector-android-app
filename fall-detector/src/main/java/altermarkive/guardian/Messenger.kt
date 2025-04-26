package altermarkive.guardian

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsManager
import android.telephony.SmsMessage
import androidx.core.content.ContextCompat
import java.util.Locale

class Messenger : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION == action) {
            val bundle = intent.extras
            if (bundle == null) {
                Guardian.say(context, android.util.Log.WARN, TAG, "Received an SMS broadcast without extras")
            } else {
                val messages = bundle["pdus"] as Array<*>
                val message = arrayOfNulls<SmsMessage>(
                    messages.size
                )
                for (i in messages.indices) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val format = bundle.getString("format")
                        message[i] = SmsMessage.createFromPdu(messages[i] as ByteArray, format)
                    } else {
                        @Suppress("DEPRECATION")
                        message[i] = SmsMessage.createFromPdu(messages[i] as ByteArray)
                    }
                }
                if (message.isNotEmpty()) {
                    val message0 = message[0]
                    if (message0 == null) {
                        Guardian.say(context, android.util.Log.WARN, TAG, "Received an SMS without content")
                        return
                    }
                    var contact = message0.originatingAddress
                    val content = message0.messageBody.uppercase(Locale.US)
                    val items = content.split(";").toTypedArray()
                    if (items.size > 1) {
                        contact = items[1]
                    }
                    if (Contact.check(context, contact)) {
                        var prevent = false
                        if (content.contains("POSITION")) {
                            Positioning.singleton?.trigger()
                            prevent = true
                        }
                        if (content.contains("ALARM")) {
                            Alarm.siren(context)
                            prevent = true
                        }
                        if (prevent) {
                            abortBroadcast()
                        }
                    }
                } else {
                    Guardian.say(context, android.util.Log.WARN, TAG, "Received empty SMS")
                }
            }
        }
    }

    companion object {
        fun sms(context: Context, contact: String?, message: String) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.SEND_SMS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val manager = SmsManager.getDefault()
                manager.sendTextMessage(contact, null, message, null, null)
            } else {
                Guardian.say(context, android.util.Log.ERROR, TAG, "ERROR: No permission to send an SMS")
            }
        }

        private val TAG: String = Messenger::class.java.simpleName
    }
}