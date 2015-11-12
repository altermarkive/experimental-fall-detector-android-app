/*
This code is free  software: you can redistribute it and/or  modify it under the
terms of the GNU Lesser General Public License as published by the Free Software
Foundation,  either version  3 of  the License,  or (at  your option)  any later
version.

This code  is distributed in the  hope that it  will be useful, but  WITHOUT ANY
WARRANTY; without even the implied warranty  of MERCHANTABILITY or FITNESS FOR A
PARTICULAR PURPOSE. See the GNU Lesser  General Public License for more details.

You should have  received a copy of the GNU  Lesser General Public License along
with code. If not, see http://www.gnu.org/licenses/.
*/
package guardian;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;

import java.util.Locale;

public class Messenger extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if ("android.provider.Telephony.SMS_RECEIVED".equals(action)) {
            Bundle bundle = intent.getExtras();
            Object messages[] = (Object[]) bundle.get("pdus");
            SmsMessage message[] = new SmsMessage[messages.length];
            for (int i = 0; i < messages.length; i++) {
                message[i] = SmsMessage.createFromPdu((byte[]) messages[i]);
            }
            if (message.length > -1) {
                String contact = message[0].getOriginatingAddress();
                String content = message[0].getMessageBody().toUpperCase(Locale.US);
                String[] items = content.split(";");
                if (items.length > 1) {
                    contact = items[1];
                }
                if (Contact.check(context, contact)) {
                    boolean prevent = false;
                    if (content.contains("POSITION")) {
                        Positioning.trigger();
                        prevent = true;
                    }
                    if (content.contains("ALARM")) {
                        Alarm.siren(context);
                        prevent = true;
                    }
                    if (prevent) {
                        this.abortBroadcast();
                    }
                }
            }
        }
    }

    public static void sms(String contact, String message) {
        SmsManager manager = SmsManager.getDefault();
        manager.sendTextMessage(contact, null, message, null, null);
    }
}
