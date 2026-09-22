package com.example.dotoday.api

import com.example.dotoday.data.TodoistTask
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Retrofit interface defining endpoints for the Todoist REST API v2.
 *
 * All network methods are suspend functions, allowing seamless integration
 * with Kotlin Coroutines without blocking the calling thread.
 */
interface TodoistApi {

    /**
     * Retrieves all active tasks for the authenticated user from Todoist.
     *
     * @param token Bearer authentication token in "Bearer <token>" format.
     * @return [Response] wrapping a list of [TodoistTask] items.
     */
    @GET("tasks")
    suspend fun getTasks(
        @Header("Authorization") token: String
    ): Response<List<TodoistTask>>

    /**
     * Creates a new task item in the user's Todoist inbox.
     *
     * @param token Bearer authentication token.
     * @param taskRequest [CreateTaskRequest] payload containing title and description.
     * @return [Response] wrapping the newly created [TodoistTask].
     */
    @POST("tasks")
    suspend fun createTask(
        @Header("Authorization") token: String,
        @Body taskRequest: CreateTaskRequest
    ): Response<TodoistTask>

    /**
     * Request payload for creating a task in Todoist.
     *
     * @property content Required title/name of the task.
     * @property description Optional task details or notes.
     */
    data class CreateTaskRequest(
        val content: String,
        val description: String? = null
    )
}
