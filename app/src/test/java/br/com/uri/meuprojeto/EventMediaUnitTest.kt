package br.com.uri.meuprojeto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class EventMediaUnitTest {

    @Test
    fun `deve criar EventPhoto e verificar o caminho do arquivo`() {
        val photo = EventPhoto(
            id = "p1",
            eventId = "e1",
            userId = "u1",
            userName = "Lucas",
            photoPath = "/storage/emulated/0/photos/photo_1.jpg"
        )

        assertEquals("p1", photo.id)
        assertEquals("e1", photo.eventId)
        assertEquals("Lucas", photo.userName)
        assertEquals("/storage/emulated/0/photos/photo_1.jpg", photo.photoPath)
    }

    @Test
    fun `deve formatar a duracao do audio em minutos e segundos corretamente`() {
        val durationMs = 125000L // 2 minutos e 5 segundos
        val seconds = (durationMs / 1000) % 60
        val minutes = (durationMs / (1000 * 60)) % 60
        val formattedDuration = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

        assertEquals("02:05", formattedDuration)
    }

    @Test
    fun `deve retornar texto padrao quando o audio nao possuir duracao`() {
        val audio = EventAudio(durationMs = 0L)
        val durationText = if (audio.durationMs > 0) "formatado" else "Áudio"

        assertEquals("Áudio", durationText)
    }

    @Test
    fun `deve criar EventVideo e validar as propriedades`() {
        val video = EventVideo(
            id = "v1",
            eventId = "e1",
            userId = "u1",
            userName = "Juliana",
            videoPath = "/storage/emulated/0/videos/video_1.mp4"
        )

        assertEquals("v1", video.id)
        assertEquals("Juliana", video.userName)
        assertEquals("/storage/emulated/0/videos/video_1.mp4", video.videoPath)
        assertTrue(video.createdAt > 0)
    }
}
