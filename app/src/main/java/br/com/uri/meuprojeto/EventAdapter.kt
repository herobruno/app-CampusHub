package br.com.uri.meuprojeto

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class EventAdapter(
    private var events: List<Event>,
    private val currentUserId: String,
    private val onSubscribeClick: (Event) -> Unit,
    private val onFavoriteClick: ((Event) -> Unit)? = null,
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    fun updateEvents(newEvents: List<Event>) {
        this.events = newEvents
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]
        holder.bind(event, currentUserId, onSubscribeClick, onFavoriteClick)
    }

    override fun getItemCount(): Int = events.size

    class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivEventImage: ImageView = itemView.findViewById(R.id.ivEventImage)
        private val btnFavorite: ImageButton = itemView.findViewById(R.id.btnFavorite)
        private val tvEventCategory: TextView = itemView.findViewById(R.id.tvEventCategory)
        private val tvEventTitle: TextView = itemView.findViewById(R.id.tvEventTitle)
        private val tvEventDate: TextView = itemView.findViewById(R.id.tvEventDate)
        private val tvEventLocation: TextView = itemView.findViewById(R.id.tvEventLocation)
        private val tvEventDescription: TextView = itemView.findViewById(R.id.tvEventDescription)
        private val tvEventCapacity: TextView = itemView.findViewById(R.id.tvEventCapacity)
        private val btnSubscribe: Button = itemView.findViewById(R.id.btnSubscribe)

        fun bind(
            event: Event,
            currentUserId: String,
            onSubscribeClick: (Event) -> Unit,
            onFavoriteClick: ((Event) -> Unit)?,
        ) {
            tvEventCategory.text = event.category
            tvEventTitle.text = event.title
            tvEventDate.text = event.date
            tvEventLocation.text = event.location
            tvEventDescription.text = event.description

            val subscriberCount = event.subscribers.size
            tvEventCapacity.text = "$subscriberCount / ${event.maxParticipants} inscritos"

            val isSubscribed = event.subscribers.contains(currentUserId)
            if (isSubscribed) {
                btnSubscribe.text = "Inscrito"
            } else {
                btnSubscribe.text = "Inscrever-se"
            }

            val isFavorite = event.favorites.contains(currentUserId)
            if (isFavorite) {
                btnFavorite.setImageResource(R.drawable.ic_heart_filled)
            } else {
                btnFavorite.setImageResource(R.drawable.ic_heart_outline)
            }

            if (event.imageUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(event.imageUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .into(ivEventImage)
            } else {
                ivEventImage.setImageResource(R.drawable.ic_launcher_background)
            }

            btnSubscribe.setOnClickListener {
                onSubscribeClick(event)
            }

            btnFavorite.setOnClickListener {
                onFavoriteClick?.invoke(event)
            }
        }
    }
}
