package com.example.dotoday.data

import com.google.gson.annotations.SerializedName

data class TodoistTask(
    val id: String,
    val content: String,
    val description: String?,
    @SerializedName("is_completed") val isCompleted: Boolean,
    @SerializedName("project_id") val projectId: String?,
    @SerializedName("created_at") val createdAt: String?
)
