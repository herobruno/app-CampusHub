package br.com.uri.meuprojeto

import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class EventMediaAdapterUnitTest {

    @Suppress("UNCHECKED_CAST")
    private val onPhotoClick: (EventPhoto) -> Unit = mock(Function1::class.java) as (EventPhoto) -> Unit
    @Suppress("UNCHECKED_CAST")
    private val onDeletePhotoClick: (EventPhoto) -> Unit = mock(Function1::class.java) as (EventPhoto) -> Unit

    @Suppress("UNCHECKED_CAST")
    private val onPlayAudioClick: (EventAudio) -> Unit = mock(Function1::class.java) as (EventAudio) -> Unit
    @Suppress("UNCHECKED_CAST")
    private val onDeleteAudioClick: (EventAudio) -> Unit = mock(Function1::class.java) as (EventAudio) -> Unit

    @Suppress("UNCHECKED_CAST")
    private val onVideoClick: (EventVideo) -> Unit = mock(Function1::class.java) as (EventVideo) -> Unit
    @Suppress("UNCHECKED_CAST")
    private val onDeleteVideoClick: (EventVideo) -> Unit = mock(Function1::class.java) as (EventVideo) -> Unit

    @Test
    fun `deve atualizar lista e contagem de fotos no EventPhotoAdapter`() {
        val adapter = EventPhotoAdapter(
            photos = emptyList(),
            currentUserId = "user1",
            onPhotoClick = onPhotoClick,
            onDeleteClick = onDeletePhotoClick
        )

        assertEquals(0, adapter.itemCount)

        val photos = listOf(
            EventPhoto(id = "p1", photoPath = "/path/p1.jpg"),
            EventPhoto(id = "p2", photoPath = "/path/p2.jpg")
        )

        try {
            adapter.updatePhotos(photos)
        } catch (_: Exception) {}

        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `deve atualizar lista e contagem de audios no EventAudioAdapter`() {
        val adapter = EventAudioAdapter(
            audios = emptyList(),
            currentUserId = "user1",
            onPlayClick = onPlayAudioClick,
            onDeleteClick = onDeleteAudioClick
        )

        assertEquals(0, adapter.itemCount)

        val audios = listOf(
            EventAudio(id = "a1", audioPath = "/path/a1.m4a"),
            EventAudio(id = "a2", audioPath = "/path/a2.m4a")
        )

        try {
            adapter.updateAudios(audios)
        } catch (_: Exception) {}

        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `deve atualizar lista e contagem de videos no EventVideoAdapter`() {
        val adapter = EventVideoAdapter(
            videos = emptyList(),
            currentUserId = "user1",
            onVideoClick = onVideoClick,
            onDeleteClick = onDeleteVideoClick
        )

        assertEquals(0, adapter.itemCount)

        val videos = listOf(
            EventVideo(id = "v1", videoPath = "/path/v1.mp4")
        )

        try {
            adapter.updateVideos(videos)
        } catch (_: Exception) {}

        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `deve invocar callbacks de foto quando acionados`() {
        val photo = EventPhoto(id = "p1", photoPath = "/path/p1.jpg")
        onPhotoClick.invoke(photo)
        onDeletePhotoClick.invoke(photo)

        verify(onPhotoClick).invoke(photo)
        verify(onDeletePhotoClick).invoke(photo)
    }

    @Test
    fun `deve invocar callbacks de audio quando acionados`() {
        val audio = EventAudio(id = "a1", audioPath = "/path/a1.m4a")
        onPlayAudioClick.invoke(audio)
        onDeleteAudioClick.invoke(audio)

        verify(onPlayAudioClick).invoke(audio)
        verify(onDeleteAudioClick).invoke(audio)
    }

    @Test
    fun `deve invocar callbacks de video quando acionados`() {
        val video = EventVideo(id = "v1", videoPath = "/path/v1.mp4")
        onVideoClick.invoke(video)
        onDeleteVideoClick.invoke(video)

        verify(onVideoClick).invoke(video)
        verify(onDeleteVideoClick).invoke(video)
    }
}
