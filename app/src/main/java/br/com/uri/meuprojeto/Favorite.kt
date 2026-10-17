package br.com.uri.meuprojeto

data class Favorite(
    val id: String = "",
    val userId: String = "",
    val eventId: String = "",
    val creatorId: String = "",
    val title: String = "",
    val date: String = "",
    val location: String = "",
    val imageUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
