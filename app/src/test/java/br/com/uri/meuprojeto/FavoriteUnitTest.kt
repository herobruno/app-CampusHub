package br.com.uri.meuprojeto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoriteUnitTest {

    @Test
    fun `deve inicializar o modelo Favorite com os valores padrao corretos`() {
        val favorite = Favorite()
        assertEquals("", favorite.id)
        assertEquals("", favorite.userId)
        assertEquals("", favorite.eventId)
        assertEquals("", favorite.creatorId)
        assertEquals("", favorite.title)
        assertEquals("OPEN", favorite.status)
    }

    @Test
    fun `deve criar o objeto Favorite com todos os dados preenchidos`() {
        val favorite = Favorite(
            id = "u10",
            userId = "u10",
            eventId = "e50",
            creatorId = "c50",
            title = "Hackathon de Mobile",
            description = "Competição de apps",
            date = "10/11/2026 - 08:00",
            location = "Lab 4",
            category = "Hackathon",
            imageUrl = "http://imagem.com/hack.jpg",
            status = "OPEN",
            maxParticipants = 30
        )

        assertEquals("e50", favorite.eventId)
        assertEquals("c50", favorite.creatorId)
        assertEquals("Hackathon de Mobile", favorite.title)
        assertEquals("Hackathon", favorite.category)
    }

    @Test
    fun `deve identificar se o evento esta no conjunto de favoritos do usuario`() {
        val favoriteIds = setOf("e50", "e51")

        assertTrue(favoriteIds.contains("e50"))
        assertFalse(favoriteIds.contains("e100"))
    }
}
