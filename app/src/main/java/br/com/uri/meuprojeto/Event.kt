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
    val imageUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)
