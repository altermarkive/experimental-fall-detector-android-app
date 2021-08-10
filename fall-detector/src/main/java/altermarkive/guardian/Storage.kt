package altermarkive.guardian

import android.util.Log
import java.io.*

object Storage {
    private val TAG = Storage::class.java.name

    internal fun size(prefix: String, file: String): Long {
        val path = File(prefix, file)
        return path.length()
    }

    internal fun readText(prefix: String, file: String): String? {
        val size = size(prefix, file)
        if (Int.MAX_VALUE < size) {
            Log.d(TAG, "Cannot read file into memory")
            return null
        }
        val buffer = ByteArray(size.toInt())
        return if (readBinary(prefix, file, buffer)) {
            String(buffer)
        } else {
            null
        }
    }

    internal fun writeText(prefix: String, file: String, content: String): String? {
        return if (writeBinary(prefix, file, content.toByteArray())) content else null
    }

    internal fun appendText(prefix: String, file: String, content: String): String? {
        return if (appendBinary(prefix, file, content.toByteArray())) content else null
    }

    internal fun readBinary(prefix: String, file: String, content: ByteArray): Boolean {
        val path = File(prefix, file)
        val input: InputStream = try {
            FileInputStream(path)
        } catch (exception: FileNotFoundException) {
            val trace: String = Log.getStackTraceString(exception)
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
            val trace: String = Log.getStackTraceString(exception)
            val message = String.format("Failed to read '%s':\n%s", file, trace)
            Log.d(TAG, message)
            result = false
        }
        try {
            input.close()
        } catch (exception: IOException) {
            val trace: String = Log.getStackTraceString(exception)
            val message = String.format("Failed to close '%s':\n%s", file, trace)
            Log.d(TAG, message)
            result = false
        }
        return result
    }

    internal fun writeBinary(prefix: String, file: String, content: ByteArray): Boolean {
        return writeBinary(prefix, file, content, false)
    }

    internal fun appendBinary(prefix: String, file: String, content: ByteArray): Boolean {
        return writeBinary(prefix, file, content, true)
    }

    private fun writeBinary(prefix: String, file: String, content: ByteArray, append: Boolean): Boolean {
        val path = File(prefix, file)
        if (File(prefix).freeSpace < content.size) {
            val message = String.format("No space left for '%s'", file)
            Log.d(TAG, message)
            return false
        }
        val output: OutputStream = try {
            FileOutputStream(path, append)
        } catch (exception: FileNotFoundException) {
            val trace: String = Log.getStackTraceString(exception)
            val message = String.format("Failed to open '%s' for writing:\n%s", file, trace)
            Log.d(TAG, message)
            return false
        }
        var result = true
        try {
            output.write(content)
        } catch (exception: IOException) {
            val trace: String = Log.getStackTraceString(exception)
            val message = String.format("Failed to write '%s':\n%s", file, trace)
            Log.d(TAG, message)
            result = false
        }
        try {
            output.close()
        } catch (exception: IOException) {
            val trace: String = Log.getStackTraceString(exception)
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
}