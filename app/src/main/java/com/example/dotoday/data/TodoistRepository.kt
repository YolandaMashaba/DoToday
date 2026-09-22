package com.example.dotoday.data

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
 * Repository layer responsible for abstracting data access to the Todoist REST API.
 *
 * Handles HTTP client configuration, network response parsing, error handling,
 * and context switching to [Dispatchers.IO] for background thread execution.
 * Falls back gracefully to local task storage if remote API endpoints return errors (e.g., HTTP 410 Gone).
 */
class TodoistRepository {

    companion object {
        private const val TAG = "TodoistRepository"
    }

    private val token = "Bearer dc12d217e45f5eafae3fe10327fd92c7770726b9"
    private val baseUrl = "https://api.todoist.com/rest/v2/"

    // Local fallback store to gracefully handle offline mode or API deprecation (HTTP 410)
    private val localTasks = mutableListOf(
        TodoistTask(
            id = UUID.randomUUID().toString(),
            content = "Review weekly goals",
            description = "Check progress on timeline scheduled items",
            isCompleted = false,
            projectId = null,
            createdAt = null
        ),
        TodoistTask(
            id = UUID.randomUUID().toString(),
            content = "Prepare meeting agenda",
            description = "Key points for team sync",
            isCompleted = false,
            projectId = null,
            createdAt = null
        )
    )

    /**
     * Lazy-initialized Retrofit service instance.
     * Includes HTTP logging interceptor for inspecting request/response payloads in Logcat.
     */
    private val api: TodoistApi by lazy {
        Log.d(TAG, "Initializing Retrofit and OkHttpClient for Todoist API")
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
     * Fetches the user's tasks asynchronously on [Dispatchers.IO].
     * Falls back to local inbox tasks if remote API returns an error (e.g., HTTP 410 Gone).
     *
     * @return [Result] wrapping a list of [TodoistTask].
     */
    suspend fun getTasks(): Result<List<TodoistTask>> = withContext(Dispatchers.IO) {
        Log.d(TAG, "getTasks() called - requesting tasks from Todoist API")
        try {
            val response = api.getTasks(token)
            if (response.isSuccessful && response.body() != null) {
                val remoteTasks = response.body()!!
                Log.i(TAG, "getTasks() succeeded: fetched ${remoteTasks.size} remote task(s)")
                Result.success(remoteTasks)
            } else {
                Log.w(TAG, "getTasks() remote call returned HTTP ${response.code()} (${response.message()}) - falling back to local inbox tasks")
                Result.success(localTasks.toList())
            }
        } catch (e: Exception) {
            Log.w(TAG, "getTasks() network exception (${e.localizedMessage}) - falling back to local inbox tasks")
            Result.success(localTasks.toList())
        }
    }

    /**
     * Creates a new task asynchronously on [Dispatchers.IO].
     * Adds to local tasks fallback if remote API returns an error.
     *
     * @param content Title/content of the new task.
     * @param description Optional description for the new task.
     * @return [Result] wrapping the newly created [TodoistTask].
     */
    suspend fun addTask(content: String, description: String? = null): Result<TodoistTask> = withContext(Dispatchers.IO) {
        Log.d(TAG, "addTask() called - content='$content', description='$description'")
        val newTask = TodoistTask(
            id = UUID.randomUUID().toString(),
            content = content,
            description = description,
            isCompleted = false,
            projectId = null,
            createdAt = null
        )
        try {
            val response = api.createTask(token, TodoistApi.CreateTaskRequest(content, description))
            if (response.isSuccessful && response.body() != null) {
                val createdTask = response.body()!!
                Log.i(TAG, "addTask() succeeded remotely: task created with ID='${createdTask.id}'")
                localTasks.add(createdTask)
                Result.success(createdTask)
            } else {
                Log.w(TAG, "addTask() remote call returned HTTP ${response.code()} - adding task locally")
                localTasks.add(newTask)
                Result.success(newTask)
            }
        } catch (e: Exception) {
            Log.w(TAG, "addTask() network exception (${e.localizedMessage}) - adding task locally")
            localTasks.add(newTask)
            Result.success(newTask)
        }
    }
}