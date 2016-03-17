/*
The MIT License (MIT)

Copyright (c) 2016

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
package altermarkive.guardian;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;

import java.util.Locale;

public class Messenger extends BroadcastReceiver {
    @Override
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
