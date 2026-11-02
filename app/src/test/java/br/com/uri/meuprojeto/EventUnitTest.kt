package br.com.uri.meuprojeto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventUnitTest {

    @Test
    fun `deve inicializar o modelo Event com os valores padrao corretos`() {
        val event = Event()
        assertEquals("", event.id)
        assertEquals("", event.creatorId)
        assertEquals("", event.creatorName)
        assertEquals("", event.title)
        assertEquals("", event.description)
        assertEquals("", event.date)
        assertEquals("", event.location)
        assertEquals("", event.category)
        assertEquals(0, event.maxParticipants)
        assertEquals(0, event.subscriberCount)
        assertEquals(0, event.favoriteCount)
        assertEquals(0, event.commentCount)
        assertEquals("OPEN", event.status)
        assertEquals(0.0, event.averageRating, 0.0)
        assertEquals(0, event.ratingCount)
        assertEquals("", event.imageUrl)
        assertFalse(event.isEnded)
    }

    @Test
    fun `deve retornar verdadeiro quando o status do evento for ENDED`() {
        val event = Event(status = "ENDED")
        assertTrue(event.isEnded)
    }

    @Test
    fun `deve retornar falso quando o status do evento for OPEN`() {
        val event = Event(status = "OPEN")
        assertFalse(event.isEnded)
    }

    @Test
    fun `deve identificar corretamente quando o evento esta lotado`() {
        val event = Event(maxParticipants = 10, subscriberCount = 10)
        val isFull = event.subscriberCount >= event.maxParticipants && event.maxParticipants > 0
        assertTrue(isFull)
    }

    @Test
    fun `deve permitir inscricao quando houver vagas disponiveis no evento`() {
        val event = Event(maxParticipants = 10, subscriberCount = 5)
        val isFull = event.subscriberCount >= event.maxParticipants && event.maxParticipants > 0
        assertFalse(isFull)
    }

    @Test
    fun `deve calcular corretamente a quantidade de vagas restantes no evento`() {
        val event = Event(maxParticipants = 50, subscriberCount = 20)
        val spotsLeft = (event.maxParticipants - event.subscriberCount).coerceAtLeast(0)
        assertEquals(30, spotsLeft)
    }

    @Test
    fun `deve retornar zero vagas restantes quando o evento ultrapassar o limite`() {
        val event = Event(maxParticipants = 10, subscriberCount = 12)
        val spotsLeft = (event.maxParticipants - event.subscriberCount).coerceAtLeast(0)
        assertEquals(0, spotsLeft)
    }

    @Test
    fun `deve verificar se o usuario atual e o criador do evento`() {
        val currentUserId = "user_123"
        val myEvent = Event(creatorId = "user_123")
        val otherEvent = Event(creatorId = "user_456")

        assertTrue(myEvent.creatorId == currentUserId)
        assertFalse(otherEvent.creatorId == currentUserId)
    }
}
