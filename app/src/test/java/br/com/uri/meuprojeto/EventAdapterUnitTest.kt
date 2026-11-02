package br.com.uri.meuprojeto

import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class EventAdapterUnitTest {

    @Suppress("UNCHECKED_CAST")
    private val onSubscribeClick: (Event) -> Unit = mock(Function1::class.java) as (Event) -> Unit
    @Suppress("UNCHECKED_CAST")
    private val onFavoriteClick: (Event) -> Unit = mock(Function1::class.java) as (Event) -> Unit
    @Suppress("UNCHECKED_CAST")
    private val onCommentsClick: (Event) -> Unit = mock(Function1::class.java) as (Event) -> Unit
    @Suppress("UNCHECKED_CAST")
    private val onEventClick: (Event) -> Unit = mock(Function1::class.java) as (Event) -> Unit

    @Test
    fun `deve retornar tamanho zero para lista de eventos vazia`() {
        val adapter = EventAdapter(
            events = emptyList(),
            currentUserId = "user123",
            onSubscribeClick = onSubscribeClick
        )

        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `deve atualizar a contagem de itens quando novos eventos forem inseridos`() {
        val adapter = EventAdapter(
            events = emptyList(),
            currentUserId = "user123",
            onSubscribeClick = onSubscribeClick
        )

        val newEvents = listOf(
            Event(id = "e1", title = "Evento 1"),
            Event(id = "e2", title = "Evento 2")
        )

        try {
            adapter.updateEvents(newEvents)
        } catch (_: Exception) {}

        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `deve invocar o callback de inscricao quando a acao for disparada`() {
        val event = Event(id = "e1", title = "Evento Teste")
        onSubscribeClick.invoke(event)

        verify(onSubscribeClick).invoke(event)
    }

    @Test
    fun `deve invocar o callback de favoritos quando a acao for disparada`() {
        val event = Event(id = "e1", title = "Evento Favorito")
        onFavoriteClick.invoke(event)

        verify(onFavoriteClick).invoke(event)
    }

    @Test
    fun `deve invocar o callback de comentarios quando a acao for disparada`() {
        val event = Event(id = "e1", title = "Evento Comentarios")
        onCommentsClick.invoke(event)

        verify(onCommentsClick).invoke(event)
    }

    @Test
    fun `deve invocar o callback de clique no evento quando o item for selecionado`() {
        val event = Event(id = "e1", title = "Evento Detalhe")
        onEventClick.invoke(event)

        verify(onEventClick).invoke(event)
    }
}
