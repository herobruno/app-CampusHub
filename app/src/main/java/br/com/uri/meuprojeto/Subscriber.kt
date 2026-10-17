package br.com.uri.meuprojeto

data class Subscriber(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val eventId: String = "",
    val creatorId: String = "",
    val title: String = "",
    val date: String = "",
    val location: String = "",
    val imageUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
