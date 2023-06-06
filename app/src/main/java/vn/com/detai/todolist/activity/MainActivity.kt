package vn.com.detai.todolist.activity

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.support.v7.widget.helper.ItemTouchHelper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.RelativeLayout
import android.widget.Toast
import vn.com.detai.R
import vn.com.detai.todolist.adapter.ListItemTouchHelper
import vn.com.detai.todolist.adapter.RecyclerViewAdapter
import vn.com.detai.todolist.adapter.RecyclerViewEmptySupport
import vn.com.detai.todolist.alarm.AlarmHelper
import vn.com.detai.todolist.alarm.AlarmReceiver
import vn.com.detai.todolist.database.DBHelper
import vn.com.detai.todolist.model.ModelTask
import vn.com.detai.todolist.settings.SettingsActivity
import vn.com.detai.todolist.utils.Interpolator
import vn.com.detai.todolist.utils.MyApplication
import vn.com.detai.todolist.utils.PreferenceHelper
import vn.com.detai.todolist.widget.WidgetProvider
import hotchemi.android.rate.AppRate
import kotterknife.bindView
import top.wefor.circularanim.CircularAnim
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity(), RecyclerViewAdapter.AdapterCallback {

    private val mRecyclerView: RecyclerViewEmptySupport by bindView(R.id.tasksList)
    private val mEmptyView: RelativeLayout by bindView(R.id.empty)
//    private val mSearchView: MaterialSearchView by bindView(R.id.search_view)
    private val mFab: FloatingActionButton by bindView(R.id.fab)

    private lateinit var mContext: Context
    private lateinit var mAdapter: RecyclerViewAdapter
    private lateinit var mHelper: DBHelper
    private lateinit var mPreferenceHelper: PreferenceHelper
    private lateinit var mNotificationManager: NotificationManager
    private lateinit var mLayoutManager: RecyclerView.LayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Set up "What's New" screen
//        val whatsNew = WhatsNew.newInstance(
//                WhatsNewItem(getString(R.string.whats_new_item_1_title), getString(R.string.whats_new_item_1_text)),
//                WhatsNewItem(getString(R.string.whats_new_item_2_title), getString(R.string.whats_new_item_2_text)),
//                WhatsNewItem(getString(R.string.whats_new_item_3_title), getString(R.string.whats_new_item_3_text))
//        )
//        whatsNew.titleColor = ContextCompat.getColor(this, R.color.colorAccent)
//        whatsNew.titleText = getString(R.string.whats_new_title)
//        whatsNew.buttonText = getString(R.string.whats_new_button_text)
//        whatsNew.buttonBackground = ContextCompat.getColor(this, R.color.colorAccent)
//        whatsNew.buttonTextColor = ContextCompat.getColor(this, R.color.white)
//        whatsNew.presentAutomatically(this@MainActivity)

        mContext = this@MainActivity
        mSearchViewIsOpen = false
        title = ""
        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager //Khởi tạo trình quản lý thông báo

        // Initialize ALARM_SERVICE
        AlarmHelper.getInstance().init(applicationContext)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        if (toolbar != null) {
            toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white))
            setSupportActionBar(toolbar)
        }

        PreferenceHelper.getInstance().init(applicationContext)
        mPreferenceHelper = PreferenceHelper.getInstance()

        RecyclerViewEmptySupport(mContext)
        mRecyclerView.setHasFixedSize(true)

        mLayoutManager = LinearLayoutManager(this)
        mRecyclerView.layoutManager = mLayoutManager
        mAdapter = RecyclerViewAdapter.getInstance()
        mRecyclerView.adapter = mAdapter
        mRecyclerView.setEmptyView(mEmptyView)

        mAdapter.registerCallback(this)

        val callback = object : ListItemTouchHelper(mAdapter, mRecyclerView) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                super.onMove(recyclerView, viewHolder, target)
                updateGeneralNotification()
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                super.onSwiped(viewHolder, direction)
                updateGeneralNotification()
            }
        }
        val touchHelper = ItemTouchHelper(callback)
        touchHelper.attachToRecyclerView(mRecyclerView)
        
        mHelper = DBHelper.getInstance(mContext)
        addTasksFromDB() //Add task vào RecyclerView

        // Show rate this app dialog
        AppRate.with(this).setInstallDays(0).setLaunchTimes(5).setRemindInterval(3).monitor()
        AppRate.showRateDialogIfMeetsConditions(this)

//        mSearchView.setOnQueryTextListener(object : MaterialSearchView.OnQueryTextListener {
//            override fun onQueryTextSubmit(query: String): Boolean {
//                Log.d(TAG, "onQueryTextSubmit")
//                return true
//            }
//
//            override fun onQueryTextChange(newText: String): Boolean {
//                findTasks(newText)
//                Log.d(TAG, "onQueryTextChange: newText = $newText")
//                return false
//            }
//        })
//
//        mSearchView.setOnSearchViewListener(object : MaterialSearchView.SearchViewListener {
//            override fun onSearchViewShown() {
//                Log.d(TAG, "onSearchViewShown!")
//                mSearchViewIsOpen = true
//                mFab.hide()
//                mFab.isEnabled = false
//                Log.d(TAG, "isSearchOpen = $mSearchViewIsOpen")
//            }
//
//            override fun onSearchViewClosed() {
//                Log.d(TAG, "onSearchViewClosed!")
//                addTasksFromDB()
//                startEmptyViewAnimation()
//                mSearchViewIsOpen = false
//                mShowAnimation = false
//
//                val handler = Handler()
//                handler.postDelayed({
//                    mFab.show()
//                    mFab.isEnabled = true
//                }, 500)
//                Log.d(TAG, "isSearchOpen = $mSearchViewIsOpen")
//            }
//        })
        // Sự kiện nhấn nút thêm ghi chú mới
        mFab.setOnClickListener { view ->
            if (mPreferenceHelper.getBoolean(PreferenceHelper.ANIMATION_IS_ON)) { 
                CircularAnim.fullActivity(this@MainActivity, view)
                        .colorOrImageRes(R.color.colorPrimary)
                        .duration(300)
                        .go {
                            val intent = Intent(this@MainActivity, AddTaskActivity::class.java)
                            // Phương pháp startActivityForResult(Intent, int) cho phép lấy đúng dữ liệu
                            // Dữ liệu được lấy từ hoạt động phương thức onActivityResult(int, int, Intent) sẽ được gọi khi AdđTaskActivity hoàn thành.
                            startActivityForResult(intent, 1)
                        }
            } else {
                val intent = Intent(this@MainActivity, AddTaskActivity::class.java)
                startActivityForResult(intent, 1)
            }
        }
        //Sự kiện trượt danh sách RecyclerView
        mRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0 && mFab.visibility == View.VISIBLE) {
                    mFab.hide()
                } else if (dy < 0 && mFab.visibility != View.VISIBLE && !mSearchViewIsOpen) {
                    mFab.show()
                }
            }
        })
        
        if (mPreferenceHelper.getBoolean(PreferenceHelper.ANIMATION_IS_ON)) {
            mFab.visibility = View.GONE

            // Starts the RecyclerView items animation
            val resId = R.anim.layout_animation
            val animation = AnimationUtils.loadLayoutAnimation(this, resId)
            mRecyclerView.layoutAnimation = animation
        } else {
            mFab.visibility = View.VISIBLE
        }
    }

   //Tìm các ghi chú trong database
    private fun findTasks(title: String) {
        mSearchViewIsOpen = true
        Log.d(TAG, "findTasks: SearchView Title = $title")
        mAdapter.removeAllTasks()
        val tasks = ArrayList<ModelTask>()

        if (title != "") {
            tasks.addAll(mHelper.getTasksForSearch(DBHelper.SELECTION_LIKE_TITLE, arrayOf("%$title%"), DBHelper.TASK_DATE_COLUMN))
        } else {
            tasks.addAll(mHelper.getAllTasks())
        }

        for (i in tasks.indices) {
            mAdapter.addTask(tasks[i])
        }
    }

   //Đọc tất cả ghi chú trong Database và thêm vào RecyclerView
    private fun addTasksFromDB() {
        mAdapter.removeAllTasks()
        val taskList = mHelper.getAllTasks()

        for (task in taskList) {
            mAdapter.addTask(task, task.position)
        }
    }

    /**
     * Starts the EmptyView animation.
     */
     
    private fun startEmptyViewAnimation() {
        if (mAdapter.itemCount == 0 && mShowAnimation) {
            mSearchViewIsOpen = false
            mRecyclerView.checkIfEmpty()
        }
    }

    /**
     * Updates widget data.
     */
    private fun updateWidget() {
        Log.d(TAG, "WIDGET IS UPDATED!")
        val intent = Intent(this, WidgetProvider::class.java)
        intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        val ids = AppWidgetManager.getInstance(this@MainActivity)
                .getAppWidgetIds(ComponentName(this@MainActivity, WidgetProvider::class.java))
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        sendBroadcast(intent)
    }

   //Cập nhật thông báo mới
    private fun updateGeneralNotification() {
        if (mPreferenceHelper.getBoolean(PreferenceHelper.GENERAL_NOTIFICATION_IS_ON)) {
            if (mAdapter.itemCount != 0) {
                showGeneralNotification()
            } else {
                removeGeneralNotification()
            }
        } else {
            removeGeneralNotification()
        }
    }

   //Cài đặt và hiển thị thông báo
    private fun showGeneralNotification() {
        val stringBuilder = StringBuilder()

        val resultIntent = Intent(this, MainActivity::class.java)
        val resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        for (task in RecyclerViewAdapter.mTaskList) {
            stringBuilder.append("• ").append(task.title)

            if (task.position < mAdapter.itemCount - 1) {
                stringBuilder.append("\n\n")
            }
        }

        var notificationTitle = ""
        when (mAdapter.itemCount % 10) {
            1 -> notificationTitle = getString(R.string.general_notification_1) + " " + mAdapter.itemCount + " " + getString(R.string.general_notification_2)

            2, 3, 4 -> notificationTitle = getString(R.string.general_notification_1) + " " + mAdapter.itemCount + " " + getString(R.string.general_notification_3)

            0, 5, 6, 7, 8, 9 -> notificationTitle = getString(R.string.general_notification_1) + " " + mAdapter.itemCount + " " + getString(R.string.general_notification_4)
        }

        //Cài đặt kênh thông báo 
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(AlarmReceiver.CHANNEL_ID, "SimpleToDo Notifications",
                    NotificationManager.IMPORTANCE_HIGH)
            channel.enableLights(true)
            channel.lightColor = Color.GREEN
            channel.enableVibration(true)
            mNotificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, AlarmReceiver.CHANNEL_ID)
                .setContentTitle(notificationTitle)
                .setContentText(stringBuilder.toString())
                .setNumber(mAdapter.itemCount)
                .setStyle(NotificationCompat.BigTextStyle().bigText(stringBuilder.toString()))
                .setColor(ContextCompat.getColor(this, R.color.colorAccent))
                .setSmallIcon(R.drawable.ic_check_circle_white_24dp)
                .setContentIntent(resultPendingIntent)
                .setOngoing(true)

        val notification = builder.build()
        mNotificationManager.notify(1, notification)
    }

    //Xóa thông báo
    private fun removeGeneralNotification() {
        mNotificationManager.cancel(1)
    }

  
     //Cập nhật dữ liệu thông báo khi người dùng nhấn "Cancel"
    override fun updateData() = updateGeneralNotification()

    override fun showFAB() = mFab.show()
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)

//        val item = menu.findItem(R.id.action_search)
//        mSearchView.setMenuItem(item)

        return true
    }
    //Nhấn vào bánh răng để chuyển sang Page Setting
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_settings) {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            return true
        }
        return false
    }

    /**
     * Đọc tất cả ghi chú từ database và so sánh với mTaskList trong RecyclerView
     * Nếu số ghi chú trong database không giống với số ghi chú trong RecyclerView
     * Tất cả các ghi chú trong database sẽ thay thế với tất cả các ghi chú trong mTaskList
     */
    override fun onStop() {
        super.onStop()

        Log.d(TAG, "onStop call!!!")
        val taskList = mHelper.getAllTasks()

        Log.d(TAG, "dbSize = ${taskList.size}, adapterSize = ${RecyclerViewAdapter.mTaskList.size}")
        if (taskList.size != RecyclerViewAdapter.mTaskList.size && !mSearchViewIsOpen && !mActivityIsShown) {

            mHelper.deleteAllTasks()

            for (task in RecyclerViewAdapter.mTaskList) {
                mHelper.saveTask(task)
            }
            mActivityIsShown = false
        }
        if (!mFab.isShown && !mSearchViewIsOpen) {
            mFab.show()
        }
        updateWidget()
    }

    override fun onResume() {
        super.onResume()

        mFab.visibility = View.GONE
        Log.d(TAG, "onResume call!!!")

        if (!mSearchViewIsOpen) {
            if (mPreferenceHelper.getBoolean(PreferenceHelper.ANIMATION_IS_ON)) {
                // Starts the FAB animation
                val handler = Handler()
                handler.postDelayed({
                    mFab.visibility = View.VISIBLE
                    val myAnim = AnimationUtils.loadAnimation(mContext, R.anim.fab_animation)
                    val interpolator = Interpolator(0.2, 20.0)
                    myAnim.interpolator = interpolator
                    mFab.startAnimation(myAnim)
                }, 300)
            } else {
                mFab.visibility = View.VISIBLE
            }
        }
        MyApplication.activityResumed()
        updateGeneralNotification()
    }

    override fun onPause() {
        super.onPause()
        MyApplication.activityPaused()
    }

    /**
     * Được gọi khi một hoạt động bạn đã khởi tạo kết thúc, cung cấp requestCode, resultCode và bất cứ dữ liệu nào từ hoạt động khởi tạo
     * requestCode: cung cấp cho startActivityForResult(), cho phép xác định kết quả đến từ đâu
     * resultCode: Được trả về bởi hoạt động con thông qua setResult()
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data == null) return

        val taskTitle = data.getStringExtra("title")
        val taskDate = data.getLongExtra("date", 0)

        val task = ModelTask()
        task.title = taskTitle
        task.date = taskDate
        task.position = mAdapter.itemCount

        // Set notification to the current task
        if (task.date != 0L && task.date <= Calendar.getInstance().timeInMillis) {
            Toast.makeText(this, getString(R.string.toast_incorrect_time), Toast.LENGTH_SHORT).show()
            task.date = 0
        } else if (task.date != 0L) {
            val alarmHelper = AlarmHelper.getInstance()
            alarmHelper.setAlarm(task)
        }

        val id = mHelper.saveTask(task)
        task.id = id
        mAdapter.addTask(task)
        updateGeneralNotification()
    }

//    override fun onBackPressed() = if (mSearchView.isSearchOpen) mSearchView.closeSearch()
//        else super.onBackPressed()

    companion object {
        var mSearchViewIsOpen: Boolean = false
        var mShowAnimation: Boolean = false
        var mActivityIsShown: Boolean = false
    }
}
