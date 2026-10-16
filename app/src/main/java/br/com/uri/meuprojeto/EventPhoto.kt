package br.com.uri.meuprojeto

data class EventPhoto(
    val id: String = "",
    val eventId: String = "",
    val userId: String = "",
    val userName: String = "",
    val photoPath: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
