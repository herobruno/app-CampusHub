package br.com.uri.meuprojeto

data class Event(
    val id: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val title: String = "",
    val description: String = "",
    val date: String = "",
    val location: String = "",
    val category: String = "",
    val maxParticipants: Int = 0,
    val subscriberCount: Int = 0,
    val favoriteCount: Int = 0,
    val commentCount: Int = 0,
    val status: String = "OPEN",
    val averageRating: Double = 0.0,
    val ratingCount: Int = 0,
    val imageUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
) {
    val isEnded: Boolean
        get() = status == "ENDED"
}
