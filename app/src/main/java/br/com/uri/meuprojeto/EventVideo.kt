package br.com.uri.meuprojeto

data class EventVideo(
    val id: String = "",
    val eventId: String = "",
    val userId: String = "",
    val userName: String = "",
    val videoPath: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
