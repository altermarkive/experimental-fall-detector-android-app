package altermarkive.guardian

import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.Throws

class Batch(private val portions: IntArray) {
    private val buffers: Array<Buffer?>
    private val stamp: Long
    fun full(index: Int): Boolean {
        return buffers[index].size() >= portions[index]
    }

    fun append(index: Int, bytes: ByteArray?, size: Int) {
        buffers[index].append(bytes, size)
    }

    fun stamp(): Long {
        return stamp
    }

    @Throws(IOException::class)
    fun zip(stream: ZipOutputStream) {
        for (i in buffers.indices) {
            val array: ByteArray = buffers[i].array()
            val size: Int = buffers[i].size()
            if (0 < size) {
                val entry = ZipEntry(String.format("data.%02d.bin", i))
                entry.size = size.toLong()
                stream.putNextEntry(entry)
                stream.write(array, 0, size)
                stream.closeEntry()
            }
        }
    }

    init {
        buffers = arrayOfNulls<Buffer>(portions.size)
        for (i in portions.indices) {
            buffers[i] = Buffer(portions[i])
        }
        stamp = System.currentTimeMillis()
    }
}