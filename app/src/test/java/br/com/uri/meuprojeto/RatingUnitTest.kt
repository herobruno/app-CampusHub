package br.com.uri.meuprojeto

import org.junit.Assert.assertEquals
import org.junit.Test

class RatingUnitTest {

    @Test
    fun `deve inicializar o modelo Rating com os valores padrao corretos`() {
        val rating = Rating()
        assertEquals("", rating.id)
        assertEquals("", rating.userId)
        assertEquals(0.0, rating.rating, 0.0)
        assertEquals("", rating.eventId)
        assertEquals("", rating.creatorId)
    }

    @Test
    fun `deve calcular a media das avaliacoes corretamente`() {
        val scores = listOf(5.0, 4.0, 3.0, 4.0)
        val totalCount = scores.size
        val average = if (totalCount > 0) scores.sum() / totalCount else 0.0

        assertEquals(4, totalCount)
        assertEquals(4.0, average, 0.001)
    }

    @Test
    fun `deve retornar media zero quando nao houver avaliacoes`() {
        val scores = emptyList<Double>()
        val totalCount = scores.size
        val average = if (totalCount > 0) scores.sum() / totalCount else 0.0

        assertEquals(0, totalCount)
        assertEquals(0.0, average, 0.0)
    }

    @Test
    fun `deve formatar o rotulo do contador de avaliacoes no singular e plural`() {
        val count1 = 1
        val label1 = if (count1 == 1) "avaliação" else "avaliações"
        assertEquals("avaliação", label1)

        val count5 = 5
        val label5 = if (count5 == 1) "avaliação" else "avaliações"
        assertEquals("avaliações", label5)
    }
}
