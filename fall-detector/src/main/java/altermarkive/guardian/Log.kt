package altermarkive.guardian

object Log {
    fun println(level: Int, tag: String, entry: String) {
        android.util.Log.println(level, tag, entry)
        Sampler.instance?.data()?.log(level, tag, entry)
    }

    @Suppress("unused")
    fun v(tag: String, entry: String) {
        android.util.Log.v(tag, entry)
        Sampler.instance?.data()?.log(android.util.Log.VERBOSE, tag, entry)
    }

    @Suppress("unused")
    fun d(tag: String, entry: String) {
        android.util.Log.d(tag, entry)
        Sampler.instance?.data()?.log(android.util.Log.DEBUG, tag, entry)
    }

    @Suppress("unused")
    fun i(tag: String, entry: String) {
        android.util.Log.i(tag, entry)
        Sampler.instance?.data()?.log(android.util.Log.INFO, tag, entry)
    }

    @Suppress("unused")
    fun w(tag: String, entry: String) {
        android.util.Log.w(tag, entry)
        Sampler.instance?.data()?.log(android.util.Log.WARN, tag, entry)
    }

    @Suppress("unused")
    fun e(tag: String, entry: String) {
        android.util.Log.e(tag, entry)
        Sampler.instance?.data()?.log(android.util.Log.ERROR, tag, entry)
    }
}