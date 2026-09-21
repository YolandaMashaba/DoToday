package com.example.dotoday.data

import com.example.dotoday.api.TodoistApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class TodoistRepository {
    private val token = "Bearer dc12d217e45f5eafae3fe10327fd92c7770726b9"
    private val baseUrl = "https://api.todoist.com/rest/v2/"

    private val api: TodoistApi by lazy {
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

    suspend fun getTasks(): Result<List<TodoistTask>> {
        return try {
            val response = api.getTasks(token)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Error: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addTask(content: String, description: String? = null): Result<TodoistTask> {
        return try {
            val response = api.createTask(token, TodoistApi.CreateTaskRequest(content, description))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
