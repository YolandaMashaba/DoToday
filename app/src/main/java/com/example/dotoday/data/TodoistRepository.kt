package com.example.dotoday.data

import android.util.Log
import com.example.dotoday.api.TodoistApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Repository layer responsible for abstracting data access to the Todoist REST API.
 *
 * Handles HTTP client configuration, network response parsing, error handling,
 * and context switching to [Dispatchers.IO] for background thread execution.
 */
class TodoistRepository {

    companion object {
        private const val TAG = "TodoistRepository"
    }

    private val token = "Bearer dc12d217e45f5eafae3fe10327fd92c7770726b9"
    private val baseUrl = "https://api.todoist.com/rest/v2/"

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
     * Fetches the user's tasks from Todoist asynchronously on [Dispatchers.IO].
     *
     * @return [Result] wrapping a list of [TodoistTask] on success, or an [Exception] on failure.
     */
    suspend fun getTasks(): Result<List<TodoistTask>> = withContext(Dispatchers.IO) {
        Log.d(TAG, "getTasks() called - requesting tasks from Todoist API")
        try {
            val response = api.getTasks(token)
            if (response.isSuccessful) {
                val tasks = response.body() ?: emptyList()
                Log.i(TAG, "getTasks() succeeded: fetched ${tasks.size} task(s)")
                Result.success(tasks)
            } else {
                val errorMsg = "HTTP ${response.code()}: ${response.message()}"
                Log.e(TAG, "getTasks() failed - $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "getTasks() threw exception: ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }

    /**
     * Creates a new task in Todoist asynchronously on [Dispatchers.IO].
     *
     * @param content Title/content of the new task.
     * @param description Optional description for the new task.
     * @return [Result] wrapping the newly created [TodoistTask] on success, or an [Exception] on failure.
     */
    suspend fun addTask(content: String, description: String? = null): Result<TodoistTask> = withContext(Dispatchers.IO) {
        Log.d(TAG, "addTask() called - content='$content', description='$description'")
        try {
            val response = api.createTask(token, TodoistApi.CreateTaskRequest(content, description))
            if (response.isSuccessful && response.body() != null) {
                val newTask = response.body()!!
                Log.i(TAG, "addTask() succeeded: task created with ID='${newTask.id}'")
                Result.success(newTask)
            } else {
                val errorMsg = "HTTP ${response.code()}: ${response.message()}"
                Log.e(TAG, "addTask() failed - $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "addTask() threw exception: ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }
}
