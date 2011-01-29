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
package altermarkive.uploader;

import android.content.Context;
import android.os.PowerManager;

public class Sampler {
    private final static String TAG = Sampler.class.getName();

    private static volatile Sampler instance = null;

    public static synchronized Sampler instance(Context context) {
        if (instance == null) {
            instance = new Sampler(context);
        }
        return instance;
    }

    private Context context;
    private Config config;
    private Data data;
    private Upload upload;

    private Sampler(Context context) {
        this.context = context;
        config = new Config(this);
        data = new Data(this);
        upload = Upload.instance();
    }

    public Context context() {
        return context;
    }

    public Config config() {
        return config;
    }

    public Data data() {
        return data;
    }

    public Upload upload() {
        return upload;
    }

    public void initiate(Context context) {
        initiate();
    }

    private native void initiate();

    static {
        System.loadLibrary("sampler");
    }
}
