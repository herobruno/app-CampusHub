package br.com.uri.meuprojeto

data class EventAudio(
    val id: String = "",
    val eventId: String = "",
    val userId: String = "",
    val userName: String = "",
    val audioPath: String = "",
    val durationMs: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)
