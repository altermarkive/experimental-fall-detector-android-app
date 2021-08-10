package altermarkive.guardian

//import java.io.ByteArrayOutputStream
//import java.io.IOException
//import java.nio.ByteBuffer
//import java.nio.ByteOrder
//import java.util.*
//import java.util.zip.ZipEntry
//import java.util.zip.ZipOutputStream
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class Data(private val root: File) : Runnable {
    private var last: String? = null
    private var db: SQLiteDatabase? = null
    private val queue = ConcurrentLinkedQueue<ContentValues>()
    private var scheduler = Executors.newScheduledThreadPool(1)

    private fun initiate() {
        scheduler.scheduleAtFixedRate(this, 5, 5, TimeUnit.SECONDS)
    }

    internal fun dispatch(type: Int, timestamp: Long, values: FloatArray) {
        val content = ContentValues()
        content.put("stamp", System.currentTimeMillis())
        content.put("timestamp", timestamp)
        content.put("type", type)
        for (i in 0..5) {
            val column = "value$i"
            if (i < values.size) {
                content.put(column, values[i])
            } else {
                content.putNull(column)
            }
        }
        queue.add(content)
    }

    internal fun log(priority: Int, tag: String, entry: String) {
        val content = ContentValues()
        content.put("stamp", System.currentTimeMillis())
        content.put("priority", priority)
        content.put("tag", tag)
        content.put("entry", entry)
        queue.add(content)
    }

    override fun run() {
        flush()
    }

    private fun flush() {
        while (queue.peek() != null) {
            queue.poll()
            val db = find()
            db.insert("data", null, queue.poll())
        }
    }

    private fun advance(current: String): SQLiteDatabase {
        this.db?.close()
        last = current
        val name = root.path + File.separator + current + ".sqlite3"
        val db = SQLiteDatabase.openOrCreateDatabase(name, null)
        db.execSQL("CREATE TABLE IF NOT EXISTS data(stamp INTEGER, timestamp INTEGER, type INTEGER, value0 REAL, value1 REAL, value2 REAL, value3 REAL, value4 REAL, value5 REAL);")
        db.execSQL("CREATE TABLE IF NOT EXISTS logs(stamp INTEGER, priority INTEGER, tag VARCHAR, entry VARCHAR);")
        this.db = db
        return db
    }

    private fun find(): SQLiteDatabase {
        val current = FORMAT.format(Date(System.currentTimeMillis()))
        val db = this.db
        return if (current != last || db == null) {
            advance(current)
        } else {
            db
        }
    }

//    private fun zip(batch: Batch) {
//        val zipped = ByteArrayOutputStream()
//        val stream = ZipOutputStream(zipped)
//        try {
//            zipTextFile(stream, "device.json")
//            zipTextFile(stream, Log.LOG_FILE)
//            Storage.writeText(Log.LOG_FILE, "")
//            batch.zip(stream)
//            stream.close()
//            val content = zipped.toByteArray()
//            val name = java.lang.String.format("data.%016X.zip", batch.stamp())
//            if (!Storage.writeBinary(name, content)) {
//                Log.e(TAG, "Failed to write a ZIP file")
//            }
//        } catch (exception: IOException) {
//            Log.e(
//                TAG, """
//     Failed to create a ZIP file:
//     ${Log.getStackTraceString(exception)}
//     """.trimIndent()
//            )
//        }
//    }
//
//    @Throws(IOException::class)
//    private fun zipTextFile(stream: ZipOutputStream, file: String) {
//        val entry = ZipEntry(file)
//        val content: String = Storage.readText(file)
//        if (content != null) {
//            val bytes = content.toByteArray()
//            entry.size = bytes.size.toLong()
//            stream.putNextEntry(entry)
//            stream.write(bytes, 0, bytes.size)
//            stream.closeEntry()
//        }
//    }

    companion object {
        private val TAG = Data::class.java.name
        private val FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }

    init {
        initiate()
    }
}