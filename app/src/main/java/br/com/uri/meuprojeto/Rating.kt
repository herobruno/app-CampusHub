package br.com.uri.meuprojeto

data class Rating(
    val id: String = "",
    val userId: String = "",
    val rating: Double = 0.0,
    val eventId: String = "",
    val creatorId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
