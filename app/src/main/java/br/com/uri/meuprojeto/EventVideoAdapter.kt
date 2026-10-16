package br.com.uri.meuprojeto

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventVideoAdapter(
    private var videos: List<EventVideo>,
    private val currentUserId: String,
    private val onVideoClick: (EventVideo) -> Unit,
    private val onDeleteClick: (EventVideo) -> Unit
) : RecyclerView.Adapter<EventVideoAdapter.VideoViewHolder>() {

    fun updateVideos(newVideos: List<EventVideo>) {
        this.videos = newVideos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_event_video, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videos[position]
        val isOwner = video.userId == currentUserId
        holder.bind(video, isOwner, onVideoClick, onDeleteClick)
    }

    override fun getItemCount(): Int = videos.size

    class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivVideoThumbnail: ImageView = itemView.findViewById(R.id.ivVideoThumbnail)
        private val tvAuthor: TextView = itemView.findViewById(R.id.tvAuthor)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val btnDeleteVideo: ImageButton = itemView.findViewById(R.id.btnDeleteVideo)

        fun bind(
            video: EventVideo,
            isOwner: Boolean,
            onVideoClick: (EventVideo) -> Unit,
            onDeleteClick: (EventVideo) -> Unit
        ) {
            tvAuthor.text = video.userName.ifEmpty { "Usuário" }

            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            tvDate.text = dateFormat.format(Date(video.createdAt))

            val videoSource: Any = if (video.videoPath.startsWith("content://") || video.videoPath.startsWith("file://")) {
                video.videoPath
            } else {
                File(video.videoPath)
            }

            Glide.with(itemView.context)
                .asBitmap()
                .load(videoSource)
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
                .into(ivVideoThumbnail)

            itemView.setOnClickListener {
                onVideoClick(video)
            }

            if (isOwner) {
                btnDeleteVideo.visibility = View.VISIBLE
                btnDeleteVideo.setOnClickListener {
                    onDeleteClick(video)
                }
            } else {
                btnDeleteVideo.visibility = View.GONE
            }
        }
    }
}
