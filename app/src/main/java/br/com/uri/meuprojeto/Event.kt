package br.com.uri.meuprojeto

data class Event(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val date: String = "",
    val location: String = "",
    val category: String = "",
    val maxParticipants: Int = 0,
    val subscribers: List<String> = emptyList(),
    val favorites: List<String> = emptyList(),
    val commentCount: Int = 0,
    val status: String = "OPEN",
    val ratings: Map<String, Double> = emptyMap(),
    val imageUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
) {
    val averageRating: Double
        get() {
            if (ratings.isEmpty()) return 0.0
            var sum = 0.0
            for (value in ratings.values) {
                sum += value
            }
            return sum / ratings.size
        }

    val ratingCount: Int
        get() = ratings.size

    val isEnded: Boolean
        get() = status == "ENDED"
}
