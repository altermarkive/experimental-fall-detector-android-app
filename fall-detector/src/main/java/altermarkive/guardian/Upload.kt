package altermarkive.guardian

import java.io.FilenameFilter
import java.io.IOException
import java.net.URL
import java.util.*
import kotlin.jvm.Volatile
import kotlin.jvm.Synchronized
import kotlin.Throws

class Upload private constructor() : Runnable {
    private var url: String? = null
    private val thread: Thread
    fun initiate(url: String?) {
        this.url = url
    }

    override fun run() {
        while (true) {
            val files: Array<String> = Storage.list(filter)
            if (files != null && 0 < files.size) {
                Arrays.sort(files)
                var item: String? = files[0]
                var hold = 1
                while (hold < 0x8000) {
                    try {
                        if (upload(url, Data.MIME, content(item), item)) {
                            Storage.delete(item)
                            item = null
                            break
                        }
                    } catch (exception: IOException) {
                        val trace: String = Log.getStackTraceString(exception)
                        val message = String.format("Failed to upload '%s':\n%s", item, trace)
                        Log.e(TAG, message)
                    }
                    Log.i(TAG, String.format("Will retry uploading in %d minutes", hold))
                    try {
                        Thread.sleep((hold * 60000).toLong())
                    } catch (exception: InterruptedException) {
                        val trace: String = Log.getStackTraceString(exception)
                        val message = String.format("Interrupted during a back-off:\n%s", trace)
                        Log.i(TAG, message)
                    }
                    hold = hold shl 1
                }
                if (item != null) {
                    val message = String.format("Failed to upload, skipping '%s'", item)
                    Log.e(TAG, message)
                }
            } else {
                try {
                    Thread.sleep(60000)
                } catch (exception: InterruptedException) {
                    val trace: String = Log.getStackTraceString(exception)
                    val message = String.format("Interrupted while waiting:\n%s", trace)
                    Log.i(TAG, message)
                }
            }
        }
    }

    fun poke() {
        thread.interrupt()
    }

    companion object {
        private val TAG = Upload::class.java.name
        private val filter = FilenameFilter { dir, name -> name.endsWith(".zip") }

        @Volatile
        private var instance: Upload? = null
        @Synchronized
        fun instance(): Upload? {
            if (instance == null) {
                instance = Upload()
            }
            return instance
        }

        private fun content(file: String?): ByteArray? {
            val size: Long = Storage.size(file)
            if (Int.MAX_VALUE < size) {
                val message = String.format("Cannot read file %s into memory", file)
                Log.e(TAG, message)
                return null
            }
            val array = ByteArray(size.toInt())
            if (!Storage.readBinary(file, array)) {
                val message = String.format("Failed to read the file %s", file)
                Log.e(TAG, message)
                return null
            }
            return array
        }

        @Throws(IOException::class)
        private fun upload(
            url: String?,
            mime: String,
            content: ByteArray?,
            thing: String?
        ): Boolean {
            if (content == null) {
                val message = String.format("Failed to read the content of '%s'", thing)
                Log.e(TAG, message)
                return false
            }
            if (url == null || url.length == 0) {
                val message = String.format("Uploading '%s' skipped (URL not configured)", thing)
                Log.i(TAG, message)
                return false
            }
            val message = String.format("Uploading '%s' to %s", thing, url)
            Log.i(TAG, message)
            val connection = URL(url).openConnection()
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", mime)
            val output = connection.getOutputStream()
            output.write(content)
            val input = connection.getInputStream()
            input.skip(input.available().toLong())
            return true
        }
    }

    init {
        thread = Thread(this)
        thread.start()
    }
}