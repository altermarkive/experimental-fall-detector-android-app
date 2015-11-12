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

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.widget.Toast;

public class Alarm {
    private static SoundPool pool = null;
    private static int id = -1;

    public static void siren(Context context) {
        if (null == pool) {
            pool = new SoundPool(5, AudioManager.STREAM_ALARM, 0);
        }
        if (-1 == id) {
            id = pool.load(context.getApplicationContext(), R.raw.alarm, 1);
        }
        loudest(context, AudioManager.STREAM_ALARM);
        pool.play(id, 1.0f, 1.0f, 1, 3, 1.0f);
    }

    public static void loudest(Context context, int stream) {
        AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int loudest = manager.getStreamMaxVolume(stream);
        manager.setStreamVolume(stream, loudest, 0);
    }

    public static void call(Context context) {
        String contact = Contact.get(context);
        if (contact != null && !"".equals(contact)) {
            Toast.makeText(context, "Calling guardian's phone number for help", Toast.LENGTH_SHORT).show();
            Intent call = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + contact));
            call.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(call);
            Telephony.handsfree(context);
        } else {
            Toast.makeText(context, "Please enter guardian's phone number in the settings", Toast.LENGTH_SHORT).show();
            siren(context);
        }
    }
}
