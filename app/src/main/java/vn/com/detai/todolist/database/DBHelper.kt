package vn.com.detai.todolist.database

import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import vn.com.detai.todolist.model.ModelTask
import java.util.*

/**
 * Lớp quản lý cơ sở dữ liệu SQLite
 */
class DBHelper private constructor(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(sqLiteDatabase: SQLiteDatabase) {
        sqLiteDatabase.execSQL(TASKS_TABLE_CREATE_SCRIPT)
    }

    override fun onUpgrade(sqLiteDatabase: SQLiteDatabase, i: Int, i1: Int) {
        sqLiteDatabase.execSQL("DROP TABLE $TASKS_TABLE")
        onCreate(sqLiteDatabase)
    }

    /**
     * Lưu ghi chú cụ thể vào database
     */
    fun saveTask(task: ModelTask): Long {
        val db = writableDatabase
        val newValues = ContentValues()

        // Don't read the id value of the task, because SQLite will generate it itself when adding new entry to the database.
        newValues.put(TASK_TITLE_COLUMN, task.title)
        newValues.put(TASK_DATE_COLUMN, task.date)
        newValues.put(TASK_POSITION_COLUMN, task.position)
        newValues.put(TASK_TIME_STAMP_COLUMN, task.timeStamp)

        val id = db.insert(TASKS_TABLE, null, newValues)
        db.close()

        Log.d(TAG, "Task with ID ($id), Title (${task.title}), Date (${task.date}), Position (${task.position}) saved to DB!")
        return id
    }

    /**
     * Cập nhật tiêu đề ghi chú và ngày vào database
     */
    fun updateTask(task: ModelTask) {
        val updatedValues = ContentValues()
        updatedValues.put(TASK_TITLE_COLUMN, task.title)
        updatedValues.put(TASK_DATE_COLUMN, task.date)

        this.writableDatabase.update(TASKS_TABLE, updatedValues, "$TASK_ID_COLUMN = ?", arrayOf(task.id.toString()))
        Log.d(TAG, "Task with ID (${task.id}), Title (${task.title}), Date (${task.date}), Position (${task.position}) updated in DB!")
    }

    /**
     * Lấy ghi chú cụ thể từ database
     */
    fun getTask(id: Long): ModelTask {
        lateinit var task: ModelTask

        val db = this.readableDatabase

        val cursor = db.query(TASKS_TABLE, arrayOf(TASK_ID_COLUMN, TASK_TITLE_COLUMN, TASK_DATE_COLUMN, TASK_POSITION_COLUMN, TASK_TIME_STAMP_COLUMN), "$TASK_ID_COLUMN = ?",
                arrayOf(id.toString()), null, null, null, null)

        if (cursor.moveToFirst()) {
            val title = cursor.getString(1)
            val date = java.lang.Long.parseLong(cursor.getString(2))
            val position = Integer.parseInt(cursor.getString(3))
            val timeStamp = java.lang.Long.parseLong(cursor.getString(4))

            task = ModelTask(id, title, date, position, timeStamp)
            Log.d(TAG, "Task with ID (${task.id}), Title (${task.title}), Date (${task.date}), Position (${task.position}) get from DB!")
        }
        cursor.close()

        return task
    }

    /**
     * Xóa ghi chú cụ thể trong database
     */
    fun deleteTask(id: Long) {
        val db = writableDatabase
        db.delete(TASKS_TABLE, "$TASK_ID_COLUMN = ?", arrayOf(id.toString()))
        db.close()

        Log.d(TAG, "Task with ID ($id) deleted from DB!")
    }

    /**
     * Lấy tất cả tác vụ trong database
     */
    fun getAllTasks(): List<ModelTask> {
        val tasksList = ArrayList<ModelTask>()
        val selectQuery = "SELECT  * FROM " + DBHelper.TASKS_TABLE

        val db = writableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                val task = ModelTask()
                task.id = java.lang.Long.parseLong(cursor.getString(0))
                task.title = cursor.getString(1)
                task.date = java.lang.Long.parseLong(cursor.getString(2))
                task.position = Integer.parseInt(cursor.getString(3))
                task.timeStamp = java.lang.Long.parseLong(cursor.getString(4))

                Log.d(TAG, "Task with ID (${task.id}), Title (${task.title}), Date (${task.date}), Position (${task.position}) extracted from DB!")
                tasksList.add(task)
            } while (cursor.moveToNext())
        }
        cursor.close()
        for (i in 0 until tasksList.size - 1) {
            for (j in 0 until tasksList.size - i - 1) {
                if (tasksList[j].position > tasksList[j + 1].position) {
                    Collections.swap(tasksList, j, j + 1)
                }
            }
        }
        return tasksList
    }

    /**
     * Xóa tất cả ghi chú trong database
     */
    fun deleteAllTasks() {
        val db = writableDatabase
        db.delete(TASKS_TABLE, null, null)
        db.close()
    }

    /**
     * Cập nhật vị trí ghi chú trong database
     */
    fun updateTaskPosition(task: ModelTask) {
        val updatedValues = ContentValues()
        updatedValues.put(TASK_POSITION_COLUMN, task.position)
        this.writableDatabase.update(TASKS_TABLE, updatedValues, "$TASK_ID_COLUMN = ?", arrayOf(task.id.toString()))

        Log.d(TAG, "Task with ID (${task.id}), Title (${task.title}), Date (${task.date}), Position (${task.position}) updated in DB!")
    }

    /**
     * Lấy ghi chú cụ thể được tìm kiếm bởi tiêu đề
     */
    fun getTasksForSearch(selection: String, selectionArgs: Array<String>, orderBy: String): List<ModelTask> {
        val tasks = ArrayList<ModelTask>()

        val db = this.readableDatabase

        val c = db.query(DBHelper.TASKS_TABLE, null, selection, selectionArgs, null, null, orderBy)

        if (c.moveToFirst()) {
            do {
                val id = c.getInt(c.getColumnIndex(DBHelper.TASK_ID_COLUMN))
                val title = c.getString(c.getColumnIndex(DBHelper.TASK_TITLE_COLUMN))
                val date = c.getLong(c.getColumnIndex(DBHelper.TASK_DATE_COLUMN))
                val position = c.getInt(c.getColumnIndex(DBHelper.TASK_POSITION_COLUMN))
                val timeStamp = c.getLong(c.getColumnIndex(DBHelper.TASK_TIME_STAMP_COLUMN))

                val modelTask = ModelTask(id.toLong(), title, date, position, timeStamp)
                tasks.add(modelTask)
            } while (c.moveToNext())
        }
        c.close()

        return tasks
    }

    companion object {

        private var mInstance: DBHelper? = null

        const val DATABASE_VERSION = 1

        const val DATABASE_NAME = "simpletodo_database"

        const val TASKS_TABLE = "tasks_table"

        const val TASK_ID_COLUMN = "_id"
        const val TASK_TITLE_COLUMN = "task_title"
        const val TASK_DATE_COLUMN = "task_date"
        const val TASK_POSITION_COLUMN = "task_position"
        const val TASK_TIME_STAMP_COLUMN = "task_time_stamp"

        const val TASKS_TABLE_CREATE_SCRIPT = ("CREATE TABLE "
                + TASKS_TABLE + " (" + TASK_ID_COLUMN + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + TASK_TITLE_COLUMN + " TEXT NOT NULL, " + TASK_DATE_COLUMN + " LONG, " + TASK_POSITION_COLUMN + " INTEGER, " + TASK_TIME_STAMP_COLUMN + " LONG);")

        const val SELECTION_LIKE_TITLE = "$TASK_TITLE_COLUMN LIKE ?"

        /**
         * Phương thức đảm bảo rằng chỉ có một DBHelper tồn tại tại bất kỳ thời điểm nào.
         * Nếu đối tượng mInstance chưa được khởi tạo, một đối tượng sẽ được tạo.
         * Nếu một cái đã được tạo thì nó sẽ được trả lại.
         */
        @Synchronized
        fun getInstance(context: Context): DBHelper {

            if (mInstance == null) {
                mInstance = DBHelper(context.applicationContext)
            }
            return mInstance as DBHelper
        }
    }
}
