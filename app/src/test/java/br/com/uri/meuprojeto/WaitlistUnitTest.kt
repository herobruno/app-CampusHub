package br.com.uri.meuprojeto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WaitlistUnitTest {

    @Test
    fun `deve inicializar o modelo WaitlistEntry com valores padrao corretos`() {
        val entry = WaitlistEntry()
        assertEquals("", entry.id)
        assertEquals("", entry.userId)
        assertEquals("", entry.userName)
        assertEquals("", entry.eventId)
        assertEquals("", entry.creatorId)
        assertEquals("", entry.title)
        assertEquals("", entry.description)
        assertEquals("", entry.date)
        assertEquals("", entry.location)
        assertEquals("", entry.category)
        assertEquals("", entry.imageUrl)
        assertEquals("WAITING", entry.status)
        assertNull(entry.offeredAt)
    }

    @Test
    fun `deve inicializar o modelo WaitlistEntry com vaga oferecida`() {
        val now = System.currentTimeMillis()
        val entry = WaitlistEntry(
            id = "user_1",
            userId = "user_1",
            userName = "Carlos Silva",
            eventId = "event_99",
            creatorId = "creator_10",
            title = "Hackathon URI",
            status = "OFFERED",
            offeredAt = now
        )

        assertEquals("user_1", entry.id)
        assertEquals("user_1", entry.userId)
        assertEquals("Carlos Silva", entry.userName)
        assertEquals("event_99", entry.eventId)
        assertEquals("creator_10", entry.creatorId)
        assertEquals("Hackathon URI", entry.title)
        assertEquals("OFFERED", entry.status)
        assertEquals(now, entry.offeredAt)
    }
}
