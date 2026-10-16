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

class EventPhotoAdapter(
    private var photos: List<EventPhoto>,
    private val currentUserId: String,
    private val onPhotoClick: (EventPhoto) -> Unit,
    private val onDeleteClick: (EventPhoto) -> Unit
) : RecyclerView.Adapter<EventPhotoAdapter.PhotoViewHolder>() {

    fun updatePhotos(newPhotos: List<EventPhoto>) {
        this.photos = newPhotos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_event_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photo = photos[position]
        val isOwner = photo.userId == currentUserId
        holder.bind(photo, isOwner, onPhotoClick, onDeleteClick)
    }

    override fun getItemCount(): Int = photos.size

    class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivPhoto: ImageView = itemView.findViewById(R.id.ivPhoto)
        private val tvAuthor: TextView = itemView.findViewById(R.id.tvAuthor)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val btnDeletePhoto: ImageButton = itemView.findViewById(R.id.btnDeletePhoto)

        fun bind(
            photo: EventPhoto,
            isOwner: Boolean,
            onPhotoClick: (EventPhoto) -> Unit,
            onDeleteClick: (EventPhoto) -> Unit
        ) {
            tvAuthor.text = photo.userName.ifEmpty { "Usuário" }

            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            tvDate.text = dateFormat.format(Date(photo.createdAt))

            val photoSource: Any = if (photo.photoPath.startsWith("content://") || photo.photoPath.startsWith("file://")) {
                photo.photoPath
            } else {
                File(photo.photoPath)
            }

            Glide.with(itemView.context)
                .load(photoSource)
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
                .into(ivPhoto)

            itemView.setOnClickListener {
                onPhotoClick(photo)
            }

            if (isOwner) {
                btnDeletePhoto.visibility = View.VISIBLE
                btnDeletePhoto.setOnClickListener {
                    onDeleteClick(photo)
                }
            } else {
                btnDeletePhoto.visibility = View.GONE
            }
        }
    }
}
