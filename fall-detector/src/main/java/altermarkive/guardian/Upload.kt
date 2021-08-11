package altermarkive.guardian

import android.content.Context
import io.ipfs.api.IPFS
import io.ipfs.api.NamedStreamable.ByteArrayWrapper
import io.ipfs.multiaddr.MultiAddress
import java.io.IOException
import java.util.*
import java.util.concurrent.Executors

class Upload internal constructor() {
    companion object {
        private val TAG = Upload::class.java.name
        private val IPFS_URL: MultiAddress = MultiAddress("/dnsaddr/ipfs.infura.io/tcp/5001/https")
        private var ipfs: IPFS? = null

        internal fun go(context: Context, root: String) {
            val ipfs = this.ipfs
            ipfs ?: return
            val zipped: Array<String>? = Storage.list(root, Storage.FILTER_ZIP)
            if (zipped != null && zipped.isNotEmpty()) {
                Arrays.sort(zipped)
                for (file in zipped) {
                    val size = Storage.size(root, file)
                    if (Int.MAX_VALUE < size) {
                        Log.e(TAG, "Cannot read file $file into memory")
                        continue
                    }
                    val array = ByteArray(size.toInt())
                    if (!Storage.readBinary(root, file, array)) {
                        Log.e(TAG, "Failed to read the file $file")
                        continue
                    }
                    val wrapper = ByteArrayWrapper(file, array)
                    try {
                        val result = ipfs.add(wrapper)[0]
                        val message =
                            "Uploaded: https://ipfs.infura.io/ipfs/${result.hash.toBase58()}"
                        Messenger.sms(context, Contact[context], message)
                    } catch (exception: IOException) {
                        val failure = android.util.Log.getStackTraceString(exception)
                        Log.e(TAG, "Failed to upload the file $file:\n $failure")
                        continue
                    }
                    Storage.delete(root, file)
                }
            }
        }
    }

    init {
        if (ipfs == null) {
            Executors.newSingleThreadExecutor().execute {
                ipfs = IPFS(IPFS_URL)
            }
        }
    }
}