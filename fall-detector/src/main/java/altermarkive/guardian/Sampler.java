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

    private Sampler(Context context) {
        this.context = context;
        config = new Config(this);
        data = new Data();
    }

    public Context context() {
        return context;
    }

    @SuppressWarnings("unused")
    public Config config() {
        return config;
    }

    public Data data() {
        return data;
    }

    public void initiate(Context context) {
        PowerManager manager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock lock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        if (!lock.isHeld()) {
            lock.acquire();
        }
        initiate();
    }

    private native void initiate();

    static {
        System.loadLibrary("sampler");
    }
}
