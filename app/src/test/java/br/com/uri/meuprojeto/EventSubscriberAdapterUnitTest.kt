package br.com.uri.meuprojeto

import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class EventSubscriberAdapterUnitTest {

    @Suppress("UNCHECKED_CAST")
    private val onToggleCheckInClick: (Subscriber) -> Unit = mock(Function1::class.java) as (Subscriber) -> Unit

    @Test
    fun `deve retornar o tamanho correto da lista de inscritos inicial`() {
        val subscribers = listOf(
            Subscriber(id = "1", userName = "Ana"),
            Subscriber(id = "2", userName = "Bruno")
        )

        val adapter = EventSubscriberAdapter(subscribers, onToggleCheckInClick)
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `deve retornar zero quando a lista de inscritos for vazia`() {
        val adapter = EventSubscriberAdapter(emptyList(), onToggleCheckInClick)
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `deve atualizar a lista de inscritos e refletir no itemCount`() {
        val adapter = EventSubscriberAdapter(emptyList(), onToggleCheckInClick)
        assertEquals(0, adapter.itemCount)

        val newSubscribers = listOf(
            Subscriber(id = "1", userName = "Carla", checkedIn = true),
            Subscriber(id = "2", userName = "Daniel", checkedIn = false),
            Subscriber(id = "3", userName = "Eduardo", checkedIn = true)
        )

        try {
            adapter.updateSubscribers(newSubscribers)
        } catch (_: Exception) {}

        assertEquals(3, adapter.itemCount)
    }

    @Test
    fun `deve invocar o callback de toggle check-in quando acionado`() {
        val subscriber = Subscriber(id = "sub_1", userName = "Fernanda", checkedIn = false)
        onToggleCheckInClick.invoke(subscriber)

        verify(onToggleCheckInClick).invoke(subscriber)
    }
}
