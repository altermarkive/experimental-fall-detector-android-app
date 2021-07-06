package altermarkive.guardian

import android.content.Context

open class DetectorAbstract {
    fun alert(context: Context) {
        Alarm.alert(context)
    }
}