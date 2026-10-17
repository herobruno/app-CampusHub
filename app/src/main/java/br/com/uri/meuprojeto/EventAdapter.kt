package br.com.uri.meuprojeto

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.util.Locale

class EventAdapter(
    private var events: List<Event>,
    private val currentUserId: String,
    private var subscribedEventIds: Set<String> = emptySet(),
    private var favoriteEventIds: Set<String> = emptySet(),
    private val onSubscribeClick: (Event) -> Unit,
    private val onFavoriteClick: ((Event) -> Unit)? = null,
    private val onCommentsClick: ((Event) -> Unit)? = null,
    private val onRateClick: ((Event) -> Unit)? = null,
    private val onEventClick: ((Event) -> Unit)? = null,
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    fun updateEvents(newEvents: List<Event>) {
        this.events = newEvents
        notifyDataSetChanged()
    }

    fun updateSubscribedEvents(subscribedIds: Set<String>) {
        this.subscribedEventIds = subscribedIds
        notifyDataSetChanged()
    }

    fun updateFavoriteEvents(favoriteIds: Set<String>) {
        this.favoriteEventIds = favoriteIds
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]
        holder.bind(event, currentUserId, subscribedEventIds, favoriteEventIds, onSubscribeClick, onFavoriteClick, onCommentsClick, onRateClick, onEventClick)
    }

    override fun getItemCount(): Int = events.size

    class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivEventImage: ImageView = itemView.findViewById(R.id.ivEventImage)
        private val btnFavorite: ImageButton = itemView.findViewById(R.id.btnFavorite)
        private val btnComments: ImageButton = itemView.findViewById(R.id.btnComments)
        private val tvCommentCount: TextView = itemView.findViewById(R.id.tvCommentCount)
        private val layoutComments: View = itemView.findViewById(R.id.layoutComments)
        private val tvEventCategory: TextView = itemView.findViewById(R.id.tvEventCategory)
        private val tvEventStatus: TextView = itemView.findViewById(R.id.tvEventStatus)
        private val tvEventTitle: TextView = itemView.findViewById(R.id.tvEventTitle)
        private val tvEventCreator: TextView = itemView.findViewById(R.id.tvEventCreator)
        private val tvEventDate: TextView = itemView.findViewById(R.id.tvEventDate)
        private val tvEventLocation: TextView = itemView.findViewById(R.id.tvEventLocation)
        private val tvEventDescription: TextView = itemView.findViewById(R.id.tvEventDescription)
        private val tvEventCapacity: TextView = itemView.findViewById(R.id.tvEventCapacity)
        private val btnSubscribe: Button = itemView.findViewById(R.id.btnSubscribe)

        private val tvRatingAverage: TextView = itemView.findViewById(R.id.tvRatingAverage)
        private val btnRateEvent: Button = itemView.findViewById(R.id.btnRateEvent)

        fun bind(
            event: Event,
            currentUserId: String,
            subscribedEventIds: Set<String>,
            favoriteEventIds: Set<String>,
            onSubscribeClick: (Event) -> Unit,
            onFavoriteClick: ((Event) -> Unit)?,
            onCommentsClick: ((Event) -> Unit)?,
            onRateClick: ((Event) -> Unit)?,
            onEventClick: ((Event) -> Unit)?,
        ) {
            tvEventCategory.text = event.category
            tvEventTitle.text = event.title

            val creator = if (event.creatorName.isNotEmpty()) event.creatorName else "URI Eventos"
            tvEventCreator.text = "Criado por: $creator"

            tvEventDate.text = event.date
            tvEventLocation.text = event.location
            tvEventDescription.text = event.description

            tvEventCapacity.text = "${event.subscriberCount} / ${event.maxParticipants} inscritos"

            val count = event.commentCount
            tvCommentCount.text = if (count == 1) "1 comentário" else "$count comentários"

            val isOwner = currentUserId.isNotEmpty() && currentUserId == event.creatorId
            val isSubscribed = subscribedEventIds.contains(event.id)

            if (isOwner) {
                btnSubscribe.text = "Organizador"
                btnSubscribe.isEnabled = false
                btnSubscribe.visibility = View.VISIBLE
            } else if (isSubscribed) {
                btnSubscribe.text = "Inscrito"
                btnSubscribe.isEnabled = true
                btnSubscribe.visibility = View.VISIBLE
            } else {
                btnSubscribe.text = "Inscrever-se"
                btnSubscribe.isEnabled = true
                btnSubscribe.visibility = View.VISIBLE
            }

            // Exibição do Status, Média e Botão de Avaliação (no lugar do botão de inscrição)
            if (event.isEnded) {
                tvEventStatus.text = "Encerrado"
                tvEventStatus.setBackgroundColor(Color.parseColor("#FFD32F2F"))

                val labelReviews = if (event.ratingCount == 1) "avaliação" else "avaliações"
                tvRatingAverage.text = String.format(Locale.getDefault(), "★ %.1f (%d %s)", event.averageRating, event.ratingCount, labelReviews)
                tvRatingAverage.visibility = View.VISIBLE

                if (isOwner) {
                    btnSubscribe.visibility = View.GONE
                    btnRateEvent.visibility = View.GONE
                } else {
                    btnSubscribe.visibility = View.GONE

                    if (isSubscribed) {
                        btnRateEvent.visibility = View.VISIBLE
                        btnRateEvent.text = "Avaliar Evento"

                        btnRateEvent.setOnClickListener {
                            onRateClick?.invoke(event)
                        }
                    } else {
                        btnRateEvent.visibility = View.GONE
                    }
                }
            } else {
                tvEventStatus.text = "Aberto"
                tvEventStatus.setBackgroundColor(Color.parseColor("#FF388E3C"))
                tvRatingAverage.visibility = View.GONE
                btnRateEvent.visibility = View.GONE
            }

            val isFavorite = favoriteEventIds.contains(event.id)
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
                if (!event.isEnded && !isOwner) {
                    onSubscribeClick(event)
                }
            }

            btnFavorite.setOnClickListener {
                onFavoriteClick?.invoke(event)
            }

            val clickCommentsListener = View.OnClickListener {
                onCommentsClick?.invoke(event)
            }
            btnComments.setOnClickListener(clickCommentsListener)
            layoutComments.setOnClickListener(clickCommentsListener)

            itemView.setOnClickListener {
                onEventClick?.invoke(event)
            }
        }
    }
}
