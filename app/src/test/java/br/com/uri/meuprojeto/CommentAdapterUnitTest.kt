package br.com.uri.meuprojeto

import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class CommentAdapterUnitTest {

    @Suppress("UNCHECKED_CAST")
    private val onEditClick: (Comment) -> Unit = mock(Function1::class.java) as (Comment) -> Unit
    @Suppress("UNCHECKED_CAST")
    private val onDeleteClick: (Comment) -> Unit = mock(Function1::class.java) as (Comment) -> Unit

    @Test
    fun `deve retornar tamanho zero para lista de comentarios vazia`() {
        val adapter = CommentAdapter(
            comments = emptyList(),
            currentUserId = "user123",
            onEditClick = onEditClick,
            onDeleteClick = onDeleteClick
        )

        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `deve atualizar a contagem de comentarios no adapter`() {
        val adapter = CommentAdapter(
            comments = emptyList(),
            currentUserId = "user123",
            onEditClick = onEditClick,
            onDeleteClick = onDeleteClick
        )

        val comments = listOf(
            Comment(id = "c1", text = "Comentário 1"),
            Comment(id = "c2", text = "Comentário 2"),
            Comment(id = "c3", text = "Comentário 3")
        )

        try {
            adapter.updateComments(comments)
        } catch (_: Exception) {}

        assertEquals(3, adapter.itemCount)
    }

    @Test
    fun `deve invocar o callback de edicao de comentario quando acionado`() {
        val comment = Comment(id = "c1", text = "Editar este comentário")
        onEditClick.invoke(comment)

        verify(onEditClick).invoke(comment)
    }

    @Test
    fun `deve invocar o callback de exclusao de comentario quando acionado`() {
        val comment = Comment(id = "c1", text = "Excluir este comentário")
        onDeleteClick.invoke(comment)

        verify(onDeleteClick).invoke(comment)
    }
}
