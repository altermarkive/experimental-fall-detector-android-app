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
import android.media.AudioManager;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;

import java.lang.reflect.Method;

public class Telephony extends BroadcastReceiver {
    private static int undo = -1;

    @Override
    public void onReceive(Context context, Intent intent) {
        TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        switch (manager.getCallState()) {
            case TelephonyManager.CALL_STATE_RINGING:
                String contact = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                if (Contact.check(context, contact)) {
                    answer(context);
                }
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                handsfree(context);
                break;
            case TelephonyManager.CALL_STATE_IDLE:
                ringing(context);
                break;
        }
    }

    public static void handsfree(Context context) {
        AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        manager.setMode(AudioManager.MODE_IN_CALL);
        manager.setSpeakerphoneOn(true);
        Alarm.loudest(context, AudioManager.STREAM_VOICE_CALL);
    }

    public static void silence(Context context) {
        try {
            TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            Method getITelephony = manager.getClass().getDeclaredMethod("getITelephony");
            getITelephony.setAccessible(true);
            Object iTelephony = getITelephony.invoke(manager);
            Method silenceRinger = iTelephony.getClass().getDeclaredMethod("silenceRinger");
            silenceRinger.invoke(iTelephony);
            undo = -1;
        } catch (Throwable ignored) {
            AudioManager manager = (AudioManager) context.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
            undo = manager.getRingerMode();
            manager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
        }
    }

    public static void ringing(Context context) {
        if (-1 != undo) {
            AudioManager manager = (AudioManager) context.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
            manager.setRingerMode(undo);
        }
    }


    private static void answer(Context context) {
        silence(context);
        try {
            TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            Method getITelephony = manager.getClass().getDeclaredMethod("getITelephony");
            getITelephony.setAccessible(true);
            Object iTelephony = getITelephony.invoke(manager);
            Method answerRingingCall = iTelephony.getClass().getDeclaredMethod("answerRingingCall");
            answerRingingCall.invoke(iTelephony);
        } catch (Throwable throwable) {
            Intent down = new Intent(Intent.ACTION_MEDIA_BUTTON);
            down.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HEADSETHOOK));
            context.sendOrderedBroadcast(down, "android.permission.CALL_PRIVILEGED");
            Intent up = new Intent(Intent.ACTION_MEDIA_BUTTON);
            up.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HEADSETHOOK));
            context.sendOrderedBroadcast(up, "android.permission.CALL_PRIVILEGED");
        }
    }
}
