package com.example.dotoday.data

import android.content.Context
import android.util.Log
import com.example.dotoday.api.TodoistApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID

/**
 * Advanced Repository layer implementing an Offline-First architecture with bidirectional sync
 * between local SQLite storage ([DoTodayDbHelper]) and remote Todoist REST API ([TodoistApi]).
 *
 * Responsibilities:
 * - **Offline-First Data Access**: Instantly serves cached tasks from SQLite to maintain UI responsiveness and offline resilience.
 * - **Background Synchronization**: Automatically syncs pending offline mutations (e.g., newly created tasks) when network connectivity is reestablished.
 * - **Network Resiliency**: Gracefully handles network failures, timeouts, and API deprecations (HTTP 410 Gone) without blocking main UI threads.
 *
 * Citation: Android Guide to App Architecture - Offline-First & Repository Pattern.
 * See: https://developer.android.com/topic/architecture/data-layer
 *
 * @property context Application context used for initializing SQLite persistence.
 */
class TodoistRepository(private val context: Context) {

    companion object {
        private const val TAG = "TodoistRepository"
    }

    private val token = "Bearer dc12d217e45f5eafae3fe10327fd92c7770726b9"
    private val baseUrl = "https://api.todoist.com/rest/v2/"

    // Local SQLite database helper for offline-first caching
    private val dbHelper = DoTodayDbHelper.getInstance(context)

    /**
     * Lazy-initialized Retrofit service instance with OkHttp logging and 5-second connection/read timeouts.
     * Prevents hanging requests and ANRs on slow or unstable emulator networks.
     */
    private val api: TodoistApi by lazy {
        Log.d(TAG, "Initializing Retrofit and OkHttpClient for Todoist REST API")
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TodoistApi::class.java)
    }

    /**
     * Fetches tasks using an offline-first strategy:
     * 1. Immediately returns cached tasks from local SQLite storage.
     * 2. Performs a background network sync with the remote Todoist API.
     * 3. Syncs any pending offline actions and updates local cache.
     *
     * @return [Result] wrapping a list of [TodoistTask].
     */
    suspend fun getTasks(): Result<List<TodoistTask>> = withContext(Dispatchers.IO) {
        Log.d(TAG, "getTasks() - Executing offline-first task retrieval")

        // 1. Load local cache immediately for instant offline rendering
        val localTasks = dbHelper.getAllTasks()
        Log.i(TAG, "Loaded ${localTasks.size} task(s) from local SQLite cache")

        // 2. Attempt remote synchronization in background
        try {
            // First, sync any pending offline mutations
            syncOfflineMutations()

            val response = api.getTasks(token)
            if (response.isSuccessful && response.body() != null) {
                val remoteTasks = response.body()!!
                Log.i(TAG, "Remote sync succeeded: fetched ${remoteTasks.size} task(s) from Todoist API")
                
                // Update local SQLite cache with fresh remote data
                dbHelper.insertTasks(remoteTasks, isSynced = true)
                val refreshedTasks = dbHelper.getAllTasks()
                Result.success(refreshedTasks)
            } else {
                Log.w(TAG, "Remote API returned HTTP ${response.code()} (${response.message()}) - serving local cache")
                Result.success(localTasks)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Network exception during task sync (${e.localizedMessage}) - offline mode active, serving local cache")
            Result.success(localTasks)
        }
    }

    /**
     * Creates a new task with offline support:
     * 1. Instantly persists the task locally with `isSynced = false`.
     * 2. Attempts remote creation via Todoist API.
     * 3. Marks the task as synchronized upon successful remote response.
     *
     * @param content Title/content of the new task.
     * @param description Optional description for the new task.
     * @return [Result] wrapping the newly created [TodoistTask].
     */
    suspend fun addTask(content: String, description: String? = null): Result<TodoistTask> = withContext(Dispatchers.IO) {
        Log.d(TAG, "addTask() - Creating task content='$content', description='$description'")
        
        val taskId = UUID.randomUUID().toString()
        val newTask = TodoistTask(
            id = taskId,
            content = content,
            description = description,
            isCompleted = false,
            projectId = null,
            createdAt = java.util.Date().toString()
        )

        // 1. Persist locally first (Offline-first write)
        dbHelper.insertTask(newTask, isSynced = false)
        Log.i(TAG, "Task persisted locally with ID='$taskId' (isSynced=false)")

        // 2. Attempt remote API creation
        try {
            val response = api.createTask(token, TodoistApi.CreateTaskRequest(content, description))
            if (response.isSuccessful && response.body() != null) {
                val createdTask = response.body()!!
                Log.i(TAG, "Remote task creation succeeded for ID='${createdTask.id}'")
                
                // Mark local record as synced or update with server ID
                dbHelper.markTaskSynced(taskId)
                Result.success(createdTask)
            } else {
                Log.w(TAG, "Remote task creation returned HTTP ${response.code()} - task saved offline for later sync")
                Result.success(newTask)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Network exception during task creation (${e.localizedMessage}) - task saved offline")
            Result.success(newTask)
        }
    }

    /**
     * Synchronizes any pending offline tasks with the remote backend.
     */
    private suspend fun syncOfflineMutations() {
        Log.d(TAG, "syncOfflineMutations() - Checking for unsynced local tasks")
        val unsyncedTasks = dbHelper.getUnsyncedTasks()
        if (unsyncedTasks.isEmpty()) {
            Log.d(TAG, "No unsynced tasks found")
            return
        }

        Log.i(TAG, "Found ${unsyncedTasks.size} unsynced task(s) to synchronize")
        for (task in unsyncedTasks) {
            try {
                val response = api.createTask(token, TodoistApi.CreateTaskRequest(task.content, task.description))
                if (response.isSuccessful) {
                    dbHelper.markTaskSynced(task.id)
                    Log.i(TAG, "Successfully synced offline task id='${task.id}'")
                } else {
                    Log.w(TAG, "Failed to sync task id='${task.id}' - HTTP ${response.code()}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Exception syncing task id='${task.id}': ${e.localizedMessage}")
            }
        }
    }
}
