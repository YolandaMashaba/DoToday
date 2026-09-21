package com.example.dotoday.api

import com.example.dotoday.data.TodoistTask
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface TodoistApi {
    @GET("tasks")
    suspend fun getTasks(
        @Header("Authorization") token: String
    ): Response<List<TodoistTask>>

    @POST("tasks")
    suspend fun createTask(
        @Header("Authorization") token: String,
        @Body taskRequest: CreateTaskRequest
    ): Response<TodoistTask>

    data class CreateTaskRequest(
        val content: String,
        val description: String? = null
    )
}
