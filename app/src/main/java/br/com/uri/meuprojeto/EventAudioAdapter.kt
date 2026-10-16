package br.com.uri.meuprojeto

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventAudioAdapter(
    private var audios: List<EventAudio>,
    private val currentUserId: String,
    private val onPlayClick: (EventAudio) -> Unit,
    private val onDeleteClick: (EventAudio) -> Unit
) : RecyclerView.Adapter<EventAudioAdapter.AudioViewHolder>() {

    private var playingAudioId: String? = null

    fun updateAudios(newAudios: List<EventAudio>) {
        this.audios = newAudios
        notifyDataSetChanged()
    }

    fun setCurrentlyPlayingId(audioId: String?) {
        this.playingAudioId = audioId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_event_audio, parent, false)
        return AudioViewHolder(view)
    }

    override fun onBindViewHolder(holder: AudioViewHolder, position: Int) {
        val audio = audios[position]
        val isPlaying = audio.id == playingAudioId
        val isOwner = audio.userId == currentUserId
        holder.bind(audio, isPlaying, isOwner, onPlayClick, onDeleteClick)
    }

    override fun getItemCount(): Int = audios.size

    class AudioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val btnPlayPause: ImageButton = itemView.findViewById(R.id.btnPlayPause)
        private val tvAuthor: TextView = itemView.findViewById(R.id.tvAuthor)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        private val btnDeleteAudio: ImageButton = itemView.findViewById(R.id.btnDeleteAudio)

        fun bind(
            audio: EventAudio,
            isPlaying: Boolean,
            isOwner: Boolean,
            onPlayClick: (EventAudio) -> Unit,
            onDeleteClick: (EventAudio) -> Unit
        ) {
            tvAuthor.text = audio.userName.ifEmpty { "Usuário" }

            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            tvDate.text = dateFormat.format(Date(audio.createdAt))

            if (audio.durationMs > 0) {
                val seconds = (audio.durationMs / 1000) % 60
                val minutes = (audio.durationMs / (1000 * 60)) % 60
                tvDuration.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
            } else {
                tvDuration.text = "Áudio"
            }

            if (isPlaying) {
                btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
            } else {
                btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
            }

            btnPlayPause.setOnClickListener {
                onPlayClick(audio)
            }

            if (isOwner) {
                btnDeleteAudio.visibility = View.VISIBLE
                btnDeleteAudio.setOnClickListener {
                    onDeleteClick(audio)
                }
            } else {
                btnDeleteAudio.visibility = View.GONE
            }
        }
    }
}
