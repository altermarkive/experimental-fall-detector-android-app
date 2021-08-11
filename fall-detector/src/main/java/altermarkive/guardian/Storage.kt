package altermarkive.guardian

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FilenameFilter
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.InputStream
import java.io.IOException
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object Storage {
    private val TAG = Storage::class.java.name

    private fun size(prefix: String, file: String): Long {
        val path = File(prefix, file)
        return path.length()
    }

    private fun readBinary(prefix: String, file: String, content: ByteArray): Boolean {
        val path = File(prefix, file)
        val input: InputStream = try {
            FileInputStream(path)
        } catch (exception: FileNotFoundException) {
            val trace: String = android.util.Log.getStackTraceString(exception)
            val message = String.format("Failed to open '%s' for reading:\n%s", file, trace)
            Log.d(TAG, message)
            return false
        }
        var result = true
        try {
            if (input.read(content) < content.size) {
                val message = String.format("Mismatched buffer size for '%s'", file)
                Log.d(TAG, message)
                result = false
            }
        } catch (exception: IOException) {
            val trace: String = android.util.Log.getStackTraceString(exception)
            val message = String.format("Failed to read '%s':\n%s", file, trace)
            Log.d(TAG, message)
            result = false
        }
        try {
            input.close()
        } catch (exception: IOException) {
            val trace: String = android.util.Log.getStackTraceString(exception)
            val message = String.format("Failed to close '%s':\n%s", file, trace)
            Log.d(TAG, message)
            result = false
        }
        return result
    }

    private fun writeBinary(
        prefix: String,
        file: String,
        content: ByteArray
    ): Boolean {
        val path = File(prefix, file)
        if (File(prefix).freeSpace < content.size) {
            val message = String.format("No space left for '%s'", file)
            Log.d(TAG, message)
            return false
        }
        val output: OutputStream = try {
            FileOutputStream(path, false)
        } catch (exception: FileNotFoundException) {
            val trace: String = android.util.Log.getStackTraceString(exception)
            val message = String.format("Failed to open '%s' for writing:\n%s", file, trace)
            Log.d(TAG, message)
            return false
        }
        var result = true
        try {
            output.write(content)
        } catch (exception: IOException) {
            val trace: String = android.util.Log.getStackTraceString(exception)
            val message = String.format("Failed to write '%s':\n%s", file, trace)
            Log.d(TAG, message)
            result = false
        }
        try {
            output.close()
        } catch (exception: IOException) {
            val trace: String = android.util.Log.getStackTraceString(exception)
            val message = String.format("Failed to close '%s':\n%s", file, trace)
            Log.d(TAG, message)
            result = false
        }
        return result
    }

    internal fun delete(prefix: String, file: String): Boolean {
        if (!File(prefix, file).delete()) {
            val message = String.format("Failed to delete '%s'", file)
            Log.d(TAG, message)
            return false
        }
        return true
    }

    internal fun list(prefix: String, filter: FilenameFilter): Array<String>? {
        return File(prefix).list(filter)
    }

    internal fun zip(prefix: String, file: String): Boolean {
        val size = size(prefix, file)
        if (size >= Integer.MAX_VALUE) {
            Log.e(TAG, "File to large to write to a ZIP file")
            return false
        }
        val array = ByteArray(size.toInt())
        if (!readBinary(prefix, file, array)) {
            return false
        }
        val zipped = ByteArrayOutputStream()
        val stream = ZipOutputStream(zipped)
        val entry = ZipEntry(file)
        entry.size = size
        try {
            stream.putNextEntry(entry)
            stream.write(array, 0, size.toInt())
            stream.closeEntry()
            stream.close()
        } catch (exception: IOException) {
            Log.e(TAG, "Failed to create a ZIP file:\n ${android.util.Log.getStackTraceString(exception)}")
            return false
        }
        val content = zipped.toByteArray()
        val name = "$file.zip"
        if (!writeBinary(prefix, name, content)) {
            Log.e(TAG, "Failed to write a ZIP file")
            return false
        }
        return true
    }

    internal fun age(prefix: String, file: String): Long {
        return File(prefix, file).lastModified()
    }

    internal val FILTER_SQLITE = FilenameFilter { _, name -> name.endsWith(".sqlite3") }
    internal val FILTER_ZIP = FilenameFilter { _, name -> name.endsWith(".zip") }
}