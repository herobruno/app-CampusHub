package br.com.uri.meuprojeto

data class WaitlistEntry(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val eventId: String = "",
    val creatorId: String = "",
    val title: String = "",
    val description: String = "",
    val date: String = "",
    val location: String = "",
    val category: String = "",
    val imageUrl: String = "",
    val status: String = "WAITING", // "WAITING", "OFFERED"
    val createdAt: Long = System.currentTimeMillis(),
    val offeredAt: Long? = null
)
