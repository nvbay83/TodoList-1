package vn.com.detai.todolist.adapter

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.TextView
import vn.com.detai.R
import vn.com.detai.todolist.activity.EditTaskActivity
import vn.com.detai.todolist.alarm.AlarmHelper
import vn.com.detai.todolist.database.DBHelper
import vn.com.detai.todolist.database.TasksOrderUpdate
import vn.com.detai.todolist.model.ModelTask
import vn.com.detai.todolist.utils.Utils
import java.util.*
import kotlin.collections.ArrayList

/**
 * Adapters connect the list views (RecyclerView for example) to it's contents (uses the Singleton pattern).
 */
class RecyclerViewAdapter private constructor() : RecyclerViewEmptySupport.EmptyAdapter<RecyclerView.ViewHolder>() {

    private lateinit var mHelper: DBHelper
    private lateinit var mContext: Context
    private lateinit var mCallback: AdapterCallback

    private val mAlarmHelper = AlarmHelper.getInstance()
    private var mCancelButtonIsClicked: Boolean = true

    /**
     * Gọi để cập nhật thông báo dữ liệu và show FAB từ lớp khác
     */
    interface AdapterCallback {
        fun updateData()
        fun showFAB()
    }

    /**
     * Đăng ký gọi lại từ một lớp khác.
     */
    fun registerCallback(callback: AdapterCallback) {
        mCallback = callback
    }

    /**
     * Thêm một item mới vào cuối danh sách
     */
    fun addTask(item: ModelTask) {
        mTaskList.add(item)
        notifyItemInserted(itemCount - 1)

        Log.d(TAG, "Task with title (${item.title}) and position (${item.position}) added to RecyclerView!")
    }

    /**
     * Thêm một item mới vào vị trí cụ thể trong danh sách
     */
    fun addTask(item: ModelTask, position: Int) {
        mTaskList.add(position, item)
        notifyItemInserted(position)

        Log.d(TAG, "Task with title (${mTaskList[position].title}) and position ($position) added to RecyclerView!")
    }

    /**
     * Cập nhật data item cụ thể trong danh sách
     */
    fun updateTask(updatedTask: ModelTask, position: Int) {
        mTaskList[position] = updatedTask
        notifyItemChanged(position)

        Log.d(TAG, "Task with title (${mTaskList[position].title}) and position ($position) updated in RecyclerView!")
    }

    /**
     * xóa item ra khỏi danh sách với Snackbar
     */
    fun removeTask(position: Int, recyclerView: RecyclerView) {
        val taskID = mTaskList[position].id
        val isRemoved = booleanArrayOf(true)
        val timeStamp = mTaskList[position].timeStamp
        mCancelButtonIsClicked = false

        Log.d(TAG, "taskID = $taskID, position = $position")
        Log.d(TAG, "Removing item from position  $position ...")

        mTaskList.removeAt(position)
        notifyItemRemoved(position)

        val snackbar = Snackbar.make(recyclerView, R.string.snackbar_remove_task, Snackbar.LENGTH_LONG)
        snackbar.setAction(R.string.snackbar_undo) {
            if (!mCancelButtonIsClicked) {
                mCancelButtonIsClicked = true
                val task = mHelper.getTask(taskID)
                addTask(task, task.position)
                isRemoved[0] = false
            }
            mCallback.updateData()
        }

        snackbar.view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            // Called when Snackbar appears on the screen.
            override fun onViewAttachedToWindow(view: View) {
                mCallback.showFAB()
            }

            // Called when Snackbar disappears from the screen.
            override fun onViewDetachedFromWindow(view: View) {
                if (isRemoved[0]) {
                    // Removes a notification and alarm
                    mAlarmHelper.removeNotification(timeStamp, mContext)
                    mAlarmHelper.removeAlarm(timeStamp)

                    // Removes a task
                    mHelper.deleteTask(taskID)
                    saveTasksOrderFromDB()
                }
            }
        })
        snackbar.show()
    }

    /**
     * Xóa item ra khỏi danh sách 
     */
    fun removeTask(position: Int) {
        val taskID = mTaskList[position].id
        val timeStamp = mTaskList[position].timeStamp

        mTaskList.removeAt(position)
        notifyItemRemoved(position)

        // Removes a notification and alarm
        mAlarmHelper.removeNotification(timeStamp, mContext)
        mAlarmHelper.removeAlarm(timeStamp)

        // Removes a task
        mHelper.deleteTask(taskID)
        saveTasksOrderFromDB()
    }

    /**
     * xóa tất cả item ra khỏi danh sách
     */
    fun removeAllTasks() {
        if (itemCount != 0) {
            mTaskList = ArrayList()
            notifyDataSetChanged()
        }
    }

    /**
     * di chuyển một item trong danh sách
     */
    fun moveTask(fromPosition: Int, toPosition: Int) {
        Log.d(TAG, "fromPosition: $fromPosition toPosition: $toPosition")

        if (fromPosition < toPosition) {
            // Move down
            for (i in fromPosition until toPosition) {
                Collections.swap(mTaskList, i, i + 1)
                mTaskList[i].position = i
                mTaskList[i + 1].position = i + 1

                Log.d(TAG, "Task with title ${mTaskList[i].title} has new position = ${mTaskList[i].position}")
                Log.d(TAG, "Task with title ${mTaskList[i + 1].title} has new position = ${mTaskList[i + 1].position}")
            }
        } else {
            // Move up
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(mTaskList, i, i - 1)
                mTaskList[i].position = i
                mTaskList[i - 1].position = i - 1

                Log.d(TAG, "Task with title ${mTaskList[i].title} has new position = ${mTaskList[i].position}")
                Log.d(TAG, "Task with title ${mTaskList[i - 1].title} has new position = ${mTaskList[i - 1].position}")
            }
        }
        notifyItemMoved(fromPosition, toPosition)
        saveTasksOrderFromRV()
    }

    /**
     * Lưu thứ tự ghi chú mới từ danh sách RecyclerView vào cơ sở dữ liệu.
     */
    private fun saveTasksOrderFromRV() {
        for (task in mTaskList) {
            task.position = mTaskList.indexOf(task)

            val order = TasksOrderUpdate(mContext)
            order.execute(task)
        }
    }

    /**
     * Lưu thứ tự ghi chú mới vào cơ sở dữ liệu.
     */
    private fun saveTasksOrderFromDB() {
        val taskList = mHelper.getAllTasks()

        for (task in taskList) {
            task.position = taskList.indexOf(task)

            val order = TasksOrderUpdate(mContext)
            order.execute(task)
        }
    }
    
    fun reloadTasks() {
        val backupList = ArrayList<ModelTask>()
        backupList.addAll(mTaskList)

        removeAllTasks()
        for (task in backupList) {
            addTask(task)
        }
    }

    /**
     * Được gọi khi RecyclerView cần một RecyclerView.ViewHolder mới của loại đã cho để đại diện cho một item
     * parent:  Chế độ xem mới sẽ được thêm vào sau khi chế độ này được liên kết với vị trí bộ điều hợp
     * viewType: Loại chế độ xem của view mới
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.model_task, parent, false)
        val title = v.findViewById<TextView>(R.id.tvTaskTitle)
        val date = v.findViewById<TextView>(R.id.tvTaskDate)

        mContext = parent.context
        mHelper = DBHelper.getInstance(mContext)

        return TaskViewHolder(v, title, date)
    }

    /**
     * Được gọi bởi RecyclerView để hiển thị dữ liệu ở vị trí đã chỉ định 
     * holder: ViewHolder cần được cập nhật để thể hiện nội dung của item tại vị trí nhất định trong tập dữ liệu
     * position: Vị trí của item trong tập dữ liệu của bộ điều hợp.
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val task = mTaskList[position]

        val taskViewHolder = holder as TaskViewHolder
        val itemView = taskViewHolder.itemView
        itemView.setOnClickListener {
            val intent = Intent(mContext, EditTaskActivity::class.java)

            intent.putExtra("id", task.id)
            intent.putExtra("title", task.title)
            intent.putExtra("position", position)
            intent.putExtra("time_stamp", task.timeStamp)

            if (task.date != 0L) {
                intent.putExtra("date", task.date)
            }
            mContext.startActivity(intent)
        }

        holder.itemView.isEnabled = true

        taskViewHolder.title.text = task.title

        if (task.date != 0L) {
            Log.d(TAG, "TASK WITH DATE")
            taskViewHolder.title.setPadding(0, 0, 0, 0)
            taskViewHolder.title.gravity = Gravity.CENTER_VERTICAL
            taskViewHolder.date.visibility = View.VISIBLE
            when {
                DateUtils.isToday(task.date) -> {
                    taskViewHolder.date.setTextColor(ContextCompat.getColor(mContext, R.color.colorPrimary))
                    taskViewHolder.date.text = mContext.getString(R.string.reminder_today) + " " + Utils.getTime(task.date)
                }
                DateUtils.isToday(task.date + DateUtils.DAY_IN_MILLIS) -> {
                    taskViewHolder.date.setTextColor(ContextCompat.getColor(mContext, R.color.red))
                    taskViewHolder.date.text = mContext.getString(R.string.reminder_yesterday) + " " + Utils.getTime(task.date)
                }
                DateUtils.isToday(task.date - DateUtils.DAY_IN_MILLIS) -> taskViewHolder.date.text = mContext.getString(R.string.reminder_tomorrow) + " " + Utils.getTime(task.date)
                task.date < Calendar.getInstance().timeInMillis -> {
                    taskViewHolder.date.setTextColor(ContextCompat.getColor(mContext, R.color.red))
                    taskViewHolder.date.text = Utils.getFullDate(task.date)
                }
                else -> taskViewHolder.date.text = Utils.getFullDate(task.date)
            }
        } else {
            Log.d(TAG, "TASK WITHOUT DATE")

            // Get the resolution of the user's screen
            val displayMetrics = DisplayMetrics()
            (mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getMetrics(displayMetrics)
            val width = displayMetrics.widthPixels
            val height = displayMetrics.heightPixels
            Log.d(TAG, "width = $width, height = $height")

            taskViewHolder.date.visibility = View.GONE
            if (width >= 1080 || height >= 1776) {
                taskViewHolder.title.setPadding(0, 27, 0, 27)
            } else if (width >= 720 || height >= 1184) {
                taskViewHolder.title.setPadding(0, 20, 0, 20)
            } else if (width >= 480 || height >= 800) {
                taskViewHolder.title.setPadding(0, 15, 0, 15)
            }
            taskViewHolder.title.gravity = Gravity.CENTER_VERTICAL
        }
    }

    /**
     * Trả về tổng số item trong tập dữ liệu của adapter
     */
    override fun getItemCount() = mTaskList.size

    /**
     * Lớp này giúp lấy tham chiếu đến từng phần tử của danh sách item cụ thể.
     */
    inner class TaskViewHolder internal constructor(itemView: View, internal var title: TextView, internal var date: TextView) : RecyclerView.ViewHolder(itemView)

    companion object {
        private var mInstance: RecyclerViewAdapter? = null

        var mTaskList: MutableList<ModelTask> = ArrayList()

        /**
         * Phương thức tĩnh này đảm bảo rằng chỉ có một RecyclerViewAdapter tồn tại tại bất kỳ thời điểm nào.
         */
        fun getInstance(): RecyclerViewAdapter {
            if (mInstance == null) {
                mInstance = RecyclerViewAdapter()
            }
            return mInstance as RecyclerViewAdapter
        }
    }
}
