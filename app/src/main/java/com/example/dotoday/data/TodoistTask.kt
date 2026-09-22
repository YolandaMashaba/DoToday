package com.example.dotoday.data

import com.google.gson.annotations.SerializedName

/**
 * Data model representing a task item returned from the Todoist REST API v2.
 *
 * Uses Gson annotations ([SerializedName]) to map snake_case JSON response fields
 * to idiomatic Kotlin camelCase properties.
 *
 * @property id Unique identifier for the Todoist task.
 * @property content The main title/text content of the task.
 * @property description Optional detailed description or notes for the task.
 * @property isCompleted Flag indicating whether the task has been completed.
 * @property projectId Optional ID of the parent Todoist project.
 * @property createdAt ISO 8601 string representation of task creation timestamp.
 */
data class TodoistTask(
    val id: String,
    val content: String,
    val description: String?,
    @SerializedName("is_completed") val isCompleted: Boolean,
    @SerializedName("project_id") val projectId: String?,
    @SerializedName("created_at") val createdAt: String?
)
