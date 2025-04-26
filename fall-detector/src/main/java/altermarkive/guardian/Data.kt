package altermarkive.guardian

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class Data(private val guardian: Guardian) {
    private val root = guardian.applicationContext.filesDir
    private var last: String? = null
    private var db: SQLiteDatabase? = null
    private val queue = ConcurrentLinkedQueue<Batch>()

    private fun initiate() {
        Upload()
        val scheduler = Executors.newScheduledThreadPool(2)
        scheduler.scheduleAtFixedRate({ sweep() }, 0, 1, TimeUnit.HOURS)
        scheduler.scheduleAtFixedRate(
            { Upload.go(guardian.applicationContext, root.path) },
            0,
            1,
            TimeUnit.HOURS
        )
        val infinite = Executors.newSingleThreadExecutor()
        infinite.execute { flush() }
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
        queue.add(Batch("data", content))
    }

    internal fun log(priority: Int, tag: String, entry: String) {
        val content = ContentValues()
        content.put("stamp", System.currentTimeMillis())
        content.put("priority", priority)
        content.put("tag", tag)
        content.put("entry", entry)
        queue.add(Batch("logs", content))
    }

    private fun flush() {
        while (true) {
            while (queue.peek() != null) {
                val db = find()
                val entry = queue.poll()
                entry ?: break
                db.insert(entry.table, null, entry.content)
            }
            Thread.sleep(1000)
        }
    }

    private fun sweep() {
        val unzipped: Array<String>? = Storage.list(root.path, Storage.FILTER_SQLITE)
        if (unzipped != null && unzipped.isNotEmpty()) {
            Arrays.sort(unzipped)
            for (file in unzipped) {
                val db = this.db
                if (db != null && db.path.endsWith(file)) {
                    continue
                }
                if (Storage.zip(root.path, file)) {
                    Storage.delete(root.path, file)
                    Storage.delete(root.path, "${file}-journal")
                }
            }
        }
        val week = 7 * 24 * 60 * 60 * 1000
        val zipped: Array<String>? = Storage.list(root.path, Storage.FILTER_ZIP)
        if (zipped != null && zipped.isNotEmpty()) {
            Arrays.sort(zipped)
            for (file in zipped) {
                if (Storage.age(root.path, file) + week < System.currentTimeMillis()) {
                    Storage.delete(root.path, file)
                }
            }
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

    companion object {
        private val FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }

    init {
        initiate()
    }
}