package br.com.uri.meuprojeto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriberUnitTest {

    @Test
    fun `deve inicializar o modelo Subscriber com os valores padrao corretos`() {
        val subscriber = Subscriber()
        assertEquals("", subscriber.id)
        assertEquals("", subscriber.userId)
        assertEquals("", subscriber.userName)
        assertEquals("", subscriber.eventId)
        assertEquals("", subscriber.creatorId)
        assertEquals("", subscriber.title)
        assertEquals("OPEN", subscriber.status)
        assertEquals(0, subscriber.maxParticipants)
    }

    @Test
    fun `deve mapear o Subscriber para o modelo de Event corretamente`() {
        val subscriber = Subscriber(
            id = "u1",
            userId = "u1",
            userName = "Mariana",
            eventId = "e100",
            creatorId = "c200",
            title = "Palestra de IA",
            description = "Inteligência Artificial no Mercado",
            date = "05/12/2026 - 19:00",
            location = "Auditório A",
            category = "Inteligência Artificial",
            imageUrl = "http://imagem.com/ia.jpg",
            status = "OPEN",
            maxParticipants = 100
        )

        val mappedEvent = Event(
            id = subscriber.eventId,
            creatorId = subscriber.creatorId,
            title = subscriber.title,
            description = subscriber.description,
            date = subscriber.date,
            location = subscriber.location,
            category = subscriber.category,
            imageUrl = subscriber.imageUrl,
            status = subscriber.status,
            maxParticipants = subscriber.maxParticipants
        )

        assertEquals("e100", mappedEvent.id)
        assertEquals("c200", mappedEvent.creatorId)
        assertEquals("Palestra de IA", mappedEvent.title)
        assertEquals("Auditório A", mappedEvent.location)
        assertEquals(100, mappedEvent.maxParticipants)
    }

    @Test
    fun `deve verificar se o ID do evento esta presente no conjunto de inscricoes`() {
        val subscribedIds = setOf("e101", "e102", "e103")

        assertTrue(subscribedIds.contains("e101"))
        assertFalse(subscribedIds.contains("e999"))
    }
}
