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
package altermarkive.guardian;

import android.content.Context;

public class Detector {
    public final static int INTERVAL_MS = 20;
    public final static int DURATION_S = 10;
    public final static int N = DURATION_S * 1000 / INTERVAL_MS;
    public final static int OFFSET_X = N * 0;
    public final static int OFFSET_Y = N * 1;
    public final static int OFFSET_Z = N * 2;
    public final static int OFFSET_X_LPF = N * 3;
    public final static int OFFSET_Y_LPF = N * 4;
    public final static int OFFSET_Z_LPF = N * 5;
    public final static int OFFSET_X_HPF = N * 6;
    public final static int OFFSET_Y_HPF = N * 7;
    public final static int OFFSET_Z_HPF = N * 8;
    public final static int OFFSET_X_D = N * 9;
    public final static int OFFSET_Y_D = N * 10;
    public final static int OFFSET_Z_D = N * 11;
    public final static int OFFSET_SV_TOT = N * 12;
    public final static int OFFSET_SV_D = N * 13;
    public final static int OFFSET_SV_MAXMIN = N * 14;
    public final static int OFFSET_Z_2 = N * 15;
    public final static int OFFSET_FALLING = N * 16;
    public final static int OFFSET_IMPACT = N * 17;
    public final static int OFFSET_LYING = N * 18;
    public final static int SIZE = N * 19;
    public final static double FALLING_WAIST_SV_TOT = 0.6;
    public final static double IMPACT_WAIST_SV_TOT = 2.0;
    public final static double IMPACT_WAIST_SV_D = 1.7;
    public final static double IMPACT_WAIST_SV_MAXMIN = 2.0;
    public final static double IMPACT_WAIST_Z_2 = 1.5;

    public native static void initiate(Context context);

    public native static void acquire();

    public native static double[] buffer();

    public native static int position();

    public native static void release();

    public static void call(Context context) {
        Alarm.call(context);
    }

    static {
        System.loadLibrary("detector");
    }
}
