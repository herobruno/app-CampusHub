package br.com.uri.meuprojeto

data class Comment(
    val id: String = "",
    val eventId: String = "",
    val userId: String = "",
    val userName: String = "",
    val text: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)
