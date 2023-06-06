package vn.com.detai.todolist.alarm

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

import vn.com.detai.todolist.model.ModelTask

import android.content.Context.NOTIFICATION_SERVICE

/**
 * Lớp khởi tạo dịch vụ báo động
 */
class AlarmHelper private constructor() {

    private lateinit var mContext: Context
    private lateinit var mAlarmManager: AlarmManager

    /**
     * Khởi tạo dịch vụ báo động
     */
    fun init(context: Context) {
        this.mContext = context
        mAlarmManager = context.applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    /**
     * Truyền dữ liệu cần thiết tới AlarmReceiver để tạo thông báo.
     */
    fun setAlarm(task: ModelTask) {
        val intent = Intent(mContext, AlarmReceiver::class.java)
        intent.putExtra("title", task.title)
        intent.putExtra("time_stamp", task.timeStamp)

        val pendingIntent = PendingIntent.getBroadcast(mContext.applicationContext, task.timeStamp.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT)

        mAlarmManager.set(AlarmManager.RTC_WAKEUP, task.date, pendingIntent)
    }

    /**
     * Xóa thông báo theo id (timeStamp).
     */
    fun removeNotification(taskTimeStamp: Long, context: Context) {
        val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(taskTimeStamp.toInt())
    }

    /**
     * Xóa báo thức theo id (timeStamp).
     */
    fun removeAlarm(taskTimeStamp: Long) {
        val intent = Intent(mContext, AlarmReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(mContext, taskTimeStamp.toInt(),
                intent, PendingIntent.FLAG_UPDATE_CURRENT)

        mAlarmManager.cancel(pendingIntent)
    }

    companion object {

        private var mInstance: AlarmHelper? = null

        /**
         * Phương thức đảm bảo rằng chỉ có một AlarmHelper tồn tại tại bất kỳ thời điểm nào.
         */
        fun getInstance(): AlarmHelper {
            if (mInstance == null) {
                mInstance = AlarmHelper()
            }
            return mInstance as AlarmHelper
        }
    }
}
