package br.com.uri.meuprojeto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommentUnitTest {

    @Test
    fun `deve inicializar o modelo Comment com os valores padrao corretos`() {
        val comment = Comment()
        assertEquals("", comment.id)
        assertEquals("", comment.eventId)
        assertEquals("", comment.userId)
        assertEquals("", comment.userName)
        assertEquals("", comment.text)
        assertTrue(comment.createdAt > 0)
    }

    @Test
    fun `deve criar o comentario com as informacoes fornecidas`() {
        val comment = Comment(
            id = "c1",
            eventId = "e1",
            userId = "u123",
            userName = "Ana Maria",
            text = "Excelente evento!",
            createdAt = 1000L
        )

        assertEquals("c1", comment.id)
        assertEquals("e1", comment.eventId)
        assertEquals("u123", comment.userId)
        assertEquals("Ana Maria", comment.userName)
        assertEquals("Excelente evento!", comment.text)
        assertEquals(1000L, comment.createdAt)
    }

    @Test
    fun `deve identificar se o comentario pertence ao usuario logado`() {
        val currentUserId = "u123"
        val myComment = Comment(userId = "u123")
        val otherComment = Comment(userId = "u999")

        assertTrue(myComment.userId == currentUserId)
        assertFalse(otherComment.userId == currentUserId)
    }

    @Test
    fun `deve extrair a inicial do nome do autor do comentario em maiusculo`() {
        val comment = Comment(userName = "carlos alberto")
        val authorName = if (comment.userName.isNotEmpty()) comment.userName else "Usuário"
        val initial = authorName.substring(0, 1).uppercase()

        assertEquals("C", initial)
    }

    @Test
    fun `deve usar nome de autor padrao quando o nome estiver vazio`() {
        val comment = Comment(userName = "")
        val authorName = if (comment.userName.isNotEmpty()) comment.userName else "Usuário"
        val initial = authorName.substring(0, 1).uppercase()

        assertEquals("Usuário", authorName)
        assertEquals("U", initial)
    }
}
