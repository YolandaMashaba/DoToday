package com.example.dotoday.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

/**
 * Robust SQLite Database Helper for offline-first task caching and synchronization in DoToday.
 *
 * This component provides local persistence using SQLite (`SQLiteOpenHelper`), allowing users
 * to view, create, and manage tasks while offline. Changes made offline are marked with [isSynced = 0]
 * and automatically synchronized with the remote REST API once network connectivity is reestablished.
 *
 * Citation: Android SQLiteOpenHelper & Offline-First Architecture Patterns.
 * See: https://developer.android.com/training/data-storage/sqlite
 *
 * @property context Application context used to initialize the SQLite database.
 */
class DoTodayDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val TAG = "DoTodayDbHelper"
        private const val DATABASE_NAME = "dotoday_tasks.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_TASKS = "tasks"
        private const val COLUMN_ID = "id"
        private const val COLUMN_CONTENT = "content"
        private const val COLUMN_DESCRIPTION = "description"
        private const val COLUMN_IS_COMPLETED = "is_completed"
        private const val COLUMN_PROJECT_ID = "project_id"
        private const val COLUMN_CREATED_AT = "created_at"
        private const val COLUMN_IS_SYNCED = "is_synced"

        @Volatile
        private var INSTANCE: DoTodayDbHelper? = null

        /**
         * Returns the thread-safe singleton instance of [DoTodayDbHelper].
         *
         * @param context Context reference.
         * @return [DoTodayDbHelper] singleton.
         */
        fun getInstance(context: Context): DoTodayDbHelper {
            return INSTANCE ?: synchronized(this) {
                Log.d(TAG, "Initializing singleton instance of DoTodayDbHelper")
                INSTANCE ?: DoTodayDbHelper(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        Log.i(TAG, "onCreate() - Creating SQLite table '$TABLE_TASKS'")
        val createTableQuery = """
            CREATE TABLE $TABLE_TASKS (
                $COLUMN_ID TEXT PRIMARY KEY,
                $COLUMN_CONTENT TEXT NOT NULL,
                $COLUMN_DESCRIPTION TEXT,
                $COLUMN_IS_COMPLETED INTEGER NOT NULL,
                $COLUMN_PROJECT_ID TEXT,
                $COLUMN_CREATED_AT TEXT,
                $COLUMN_IS_SYNCED INTEGER NOT NULL DEFAULT 1
            )
        """.trimIndent()
        db.execSQL(createTableQuery)
        Log.i(TAG, "Table '$TABLE_TASKS' created successfully")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.w(TAG, "onUpgrade() - Upgrading database from version $oldVersion to $newVersion. Dropping old data.")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TASKS")
        onCreate(db)
    }

    /**
     * Retrieves all cached tasks stored in the local SQLite database.
     *
     * @return List of [TodoistTask] models parsed from local database rows.
     */
    fun getAllTasks(): List<TodoistTask> {
        Log.d(TAG, "getAllTasks() - Querying local SQLite database for tasks")
        val tasks = mutableListOf<TodoistTask>()
        val db = readableDatabase
        val cursor = db.query(TABLE_TASKS, null, null, null, null, null, "$COLUMN_CREATED_AT DESC")

        cursor.use {
            val idIdx = it.getColumnIndex(COLUMN_ID)
            val contentIdx = it.getColumnIndex(COLUMN_CONTENT)
            val descIdx = it.getColumnIndex(COLUMN_DESCRIPTION)
            val completedIdx = it.getColumnIndex(COLUMN_IS_COMPLETED)
            val projectIdIdx = it.getColumnIndex(COLUMN_PROJECT_ID)
            val createdAtIdx = it.getColumnIndex(COLUMN_CREATED_AT)

            while (it.moveToNext()) {
                val id = if (idIdx != -1) it.getString(idIdx) else ""
                val content = if (contentIdx != -1) it.getString(contentIdx) else ""
                val description = if (descIdx != -1) it.getString(descIdx) else null
                val isCompleted = if (completedIdx != -1) it.getInt(completedIdx) == 1 else false
                val projectId = if (projectIdIdx != -1) it.getString(projectIdIdx) else null
                val createdAt = if (createdAtIdx != -1) it.getString(createdAtIdx) else null

                tasks.add(TodoistTask(id, content, description, isCompleted, projectId, createdAt))
            }
        }
        Log.i(TAG, "getAllTasks() - Retrieved ${tasks.size} task(s) from local SQLite database")
        return tasks
    }

    /**
     * Inserts or replaces a single task in the local SQLite database.
     *
     * @param task [TodoistTask] to store.
     * @param isSynced Boolean flag indicating whether the task is synchronized with the remote API.
     */
    fun insertTask(task: TodoistTask, isSynced: Boolean = true) {
        Log.d(TAG, "insertTask() - Inserting task id='${task.id}', content='${task.content}', isSynced=$isSynced")
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ID, task.id)
            put(COLUMN_CONTENT, task.content)
            put(COLUMN_DESCRIPTION, task.description)
            put(COLUMN_IS_COMPLETED, if (task.isCompleted) 1 else 0)
            put(COLUMN_PROJECT_ID, task.projectId)
            put(COLUMN_CREATED_AT, task.createdAt)
            put(COLUMN_IS_SYNCED, if (isSynced) 1 else 0)
        }
        db.insertWithOnConflict(TABLE_TASKS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    /**
     * Inserts a list of tasks into the local SQLite database in a batch transaction.
     *
     * @param tasks List of [TodoistTask] items.
     * @param isSynced Synchronization flag.
     */
    fun insertTasks(tasks: List<TodoistTask>, isSynced: Boolean = true) {
        Log.d(TAG, "insertTasks() - Batch inserting ${tasks.size} task(s) into SQLite database")
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (task in tasks) {
                val values = ContentValues().apply {
                    put(COLUMN_ID, task.id)
                    put(COLUMN_CONTENT, task.content)
                    put(COLUMN_DESCRIPTION, task.description)
                    put(COLUMN_IS_COMPLETED, if (task.isCompleted) 1 else 0)
                    put(COLUMN_PROJECT_ID, task.projectId)
                    put(COLUMN_CREATED_AT, task.createdAt)
                    put(COLUMN_IS_SYNCED, if (isSynced) 1 else 0)
                }
                db.insertWithOnConflict(TABLE_TASKS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
            Log.i(TAG, "Batch insert of ${tasks.size} task(s) completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error during batch task insertion: ${e.localizedMessage}", e)
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Retrieves all tasks that are pending synchronization with the remote backend.
     *
     * @return List of unsynced [TodoistTask] items.
     */
    fun getUnsyncedTasks(): List<TodoistTask> {
        Log.d(TAG, "getUnsyncedTasks() - Querying unsynced local tasks")
        val tasks = mutableListOf<TodoistTask>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_TASKS,
            null,
            "$COLUMN_IS_SYNCED = 0",
            null,
            null,
            null,
            null
        )

        cursor.use {
            val idIdx = it.getColumnIndex(COLUMN_ID)
            val contentIdx = it.getColumnIndex(COLUMN_CONTENT)
            val descIdx = it.getColumnIndex(COLUMN_DESCRIPTION)
            val completedIdx = it.getColumnIndex(COLUMN_IS_COMPLETED)
            val projectIdIdx = it.getColumnIndex(COLUMN_PROJECT_ID)
            val createdAtIdx = it.getColumnIndex(COLUMN_CREATED_AT)

            while (it.moveToNext()) {
                val id = if (idIdx != -1) it.getString(idIdx) else ""
                val content = if (contentIdx != -1) it.getString(contentIdx) else ""
                val description = if (descIdx != -1) it.getString(descIdx) else null
                val isCompleted = if (completedIdx != -1) it.getInt(completedIdx) == 1 else false
                val projectId = if (projectIdIdx != -1) it.getString(projectIdIdx) else null
                val createdAt = if (createdAtIdx != -1) it.getString(createdAtIdx) else null

                tasks.add(TodoistTask(id, content, description, isCompleted, projectId, createdAt))
            }
        }
        Log.i(TAG, "Found ${tasks.size} unsynced task(s)")
        return tasks
    }

    /**
     * Marks a specific task as synchronized in the local database.
     *
     * @param taskId Unique identifier of the task.
     */
    fun markTaskSynced(taskId: String) {
        Log.d(TAG, "markTaskSynced() - Marking task id='$taskId' as synchronized")
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_IS_SYNCED, 1)
        }
        db.update(TABLE_TASKS, values, "$COLUMN_ID = ?", arrayOf(taskId))
    }

    /**
     * Clears all tasks from the local database.
     */
    fun clearAll() {
        Log.w(TAG, "clearAll() - Deleting all records from '$TABLE_TASKS'")
        val db = writableDatabase
        db.delete(TABLE_TASKS, null, null)
    }
}
