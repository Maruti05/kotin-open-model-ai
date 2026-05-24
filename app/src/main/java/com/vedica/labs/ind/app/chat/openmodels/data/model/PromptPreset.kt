package com.vedica.labs.ind.app.chat.openmodels.data.model

data class PromptPreset(
    val id: String,
    val title: String,
    val description: String,
    val systemPrompt: String,
    val category: String,
    val iconName: String = "code"
)
