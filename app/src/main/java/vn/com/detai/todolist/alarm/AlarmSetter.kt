package vn.com.detai.todolist.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import java.util.ArrayList

import vn.com.detai.todolist.database.DBHelper
import vn.com.detai.todolist.model.ModelTask

/**
 * Lớp khôi phục tất cả các thông báo sau khi khởi động lại thiết bị.
 */
class AlarmSetter : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val helper = DBHelper.getInstance(context)

        AlarmHelper.getInstance().init(context)
        val alarmHelper = AlarmHelper.getInstance()

        val tasks = ArrayList<ModelTask>()
        tasks.addAll(helper.getAllTasks())

        for (task in tasks) {
            if (task.date != 0L) {
                alarmHelper.setAlarm(task)
            }
        }
    }
}
