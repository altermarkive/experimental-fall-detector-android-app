package altermarkive.guardian

//import java.io.ByteArrayOutputStream
//import java.io.IOException
//import java.nio.ByteBuffer
//import java.nio.ByteOrder
//import java.util.*
//import java.util.zip.ZipEntry
//import java.util.zip.ZipOutputStream
import android.database.sqlite.SQLiteDatabase
import java.text.SimpleDateFormat
import java.util.*
import kotlin.jvm.Synchronized
//import kotlin.Throws

class Data { //}: Runnable {
//    private val bytes = ByteBuffer.allocate(64).order(ByteOrder.LITTLE_ENDIAN)
//    private val queue: Vector<Batch> = Vector<Batch>()
//    private var portions = IntArray(0)
//    private var storing = 0
//    private var schedule: Long = 0

//    fun initiate(sizes: IntArray, periods: IntArray, storing: Int) {
//        this.storing = 0
//        // Calculate the batch portions
//        val portions = IntArray(sizes.size)
//        Arrays.fill(portions, 0)
//        for (i in sizes.indices) {
//            if (periods[i] != 0) {
//                portions[i] = sizes[i] * Math.ceil(
//                    storing.toDouble() / periods[i].toDouble()
//                ).toInt()
//            }
//        }
//        // Initiate the batch queue
//        queue.clear()
//        this.portions = portions
//        this.storing = storing
//        advance()
//    }

    @Synchronized
    fun dispatch(type: Int, stamp: Long, values: FloatArray) {
//        val now = System.currentTimeMillis()
//        val date = Date(now)
//        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'")
//        format.format(date)

//        if (storing != 0) {
//            bytes.position(0)
//            bytes.putLong(stamp)
//            for (i in 0 until axes) {
//                bytes.putFloat(values[i])
//            }
//            val batch: Batch = find(index)
//            batch.append(index, bytes.array(), bytes.position())
//            synchronized(this) { notifyAll() }
//        }
    }

//    override fun run() {
//        while (true) {
//            while (storing != 0 && 1 < queue.size) {
//                val batch: Batch = queue.removeAt(0)
//                zip(batch)
//            }
//            synchronized(this) {
//                try {
//                    wait()
//                } catch (exception: InterruptedException) {
//                    val trace: String = Log.getStackTraceString(exception)
//                    val message = String.format("Data thread was interrupted:\n%s", trace)
//                    Log.d(TAG, message)
//                }
//            }
//        }
//    }

//    private fun advance(): Batch {
//        val batch = Batch(portions)
//        queue.add(batch)
//        if (schedule == 0L) {
//            schedule = System.currentTimeMillis()
//        }
//        schedule += storing.toLong()
//        return batch
//    }

//    private fun find(index: Int): Batch {
//        val last: Batch = queue.lastElement()
//        return if (schedule < System.currentTimeMillis() || last.full(index)) {
//            advance()
//        } else {
//            last
//        }
//    }

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
        const val MIME = "application/zip"
    }

    init {
//        Thread(this).start()
    }
}