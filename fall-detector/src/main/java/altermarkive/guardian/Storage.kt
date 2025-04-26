package altermarkive.guardian

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
        val path = File(prefix, file)
        val input: InputStream = try {
            FileInputStream(path)
        } catch (exception: FileNotFoundException) {
            val trace: String = android.util.Log.getStackTraceString(exception)
            Log.d(TAG, "Failed to open '$file' for reading:\n$trace")
            return false
        }
        val output: OutputStream = try {
            FileOutputStream(File(prefix, "$file.zip"))
        } catch (exception: FileNotFoundException) {
            val trace: String = android.util.Log.getStackTraceString(exception)
            Log.d(TAG, "Failed to open '$file.zip' for writing:\n$trace")
            return false
        }
        val zipped = ZipOutputStream(output)
        val entry = ZipEntry(file)
        try {
            zipped.putNextEntry(entry)
            val buffer = ByteArray(4096)
            var size: Int
            while (input.read(buffer).also { size = it } != -1) zipped.write(buffer, 0, size)
            zipped.flush()
            zipped.closeEntry()
            zipped.close()
            output.flush()
        } catch (exception: IOException) {
            val trace: String = android.util.Log.getStackTraceString(exception)
            Log.e(TAG, "Failed to compress the file $file:\n${trace}")
            return false
        }
        var result = true
        try {
            input.close()
        } catch (exception: IOException) {
            val trace: String = android.util.Log.getStackTraceString(exception)
            Log.e(TAG, "Failed to close the file $file:\n${trace}")
            result = false
        }
        try {
            output.close()
        } catch (exception: IOException) {
            val trace: String = android.util.Log.getStackTraceString(exception)
            Log.e(TAG, "Failed to close the file $file.zip:\n${trace}")
            result = false
        }
        return result
    }

    internal fun age(prefix: String, file: String): Long {
        return File(prefix, file).lastModified()
    }

    internal val FILTER_SQLITE = FilenameFilter { _, name -> name.endsWith(".sqlite3") }
    internal val FILTER_ZIP = FilenameFilter { _, name -> name.endsWith(".zip") }
}