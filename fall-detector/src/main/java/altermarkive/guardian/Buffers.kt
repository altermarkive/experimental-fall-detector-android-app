package altermarkive.guardian

class Buffers(count: Int, size: Int, var position: Int, value: Double) {
    val buffers: Array<DoubleArray> = Array(count) { DoubleArray(size) { value } }

    fun copyInto(buffers: Buffers) {
        if (this.buffers.size != buffers.buffers.size) {
            return
        }
        for (i: Int in this.buffers.indices) {
            if (this.buffers[i].size != buffers.buffers[i].size) {
                return
            }
        }
        for (i: Int in this.buffers.indices) {
                this.buffers[i].copyInto(buffers.buffers[i])
        }
        buffers.position = position
    }
}