package br.com.uri.meuprojeto

data class Favorite(
    val id: String = "",
    val userId: String = "",
    val eventId: String = "",
    val creatorId: String = "",
    val title: String = "",
    val description: String = "",
    val date: String = "",
    val location: String = "",
    val category: String = "",
    val imageUrl: String = "",
    val status: String = "OPEN",
    val maxParticipants: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
