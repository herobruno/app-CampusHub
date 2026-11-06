package br.com.uri.meuprojeto

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class EventSubscriberAdapter(
    private var subscribers: List<Subscriber>,
    private val onToggleCheckInClick: (Subscriber) -> Unit
) : RecyclerView.Adapter<EventSubscriberAdapter.SubscriberViewHolder>() {

    fun updateSubscribers(newSubscribers: List<Subscriber>) {
        this.subscribers = newSubscribers
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubscriberViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_event_subscriber, parent, false)
        return SubscriberViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubscriberViewHolder, position: Int) {
        val subscriber = subscribers[position]
        holder.bind(subscriber, onToggleCheckInClick)
    }

    override fun getItemCount(): Int = subscribers.size

    class SubscriberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvUserInitial: TextView = itemView.findViewById(R.id.tvUserInitial)
        private val tvSubscriberName: TextView = itemView.findViewById(R.id.tvSubscriberName)
        private val tvCheckInStatus: TextView = itemView.findViewById(R.id.tvCheckInStatus)
        private val btnToggleCheckIn: Button = itemView.findViewById(R.id.btnToggleCheckIn)

        fun bind(subscriber: Subscriber, onToggleCheckInClick: (Subscriber) -> Unit) {
            val name = subscriber.userName.ifEmpty { "Usuário" }
            tvSubscriberName.text = name

            if (name.isNotEmpty()) {
                tvUserInitial.text = name.substring(0, 1).uppercase()
            } else {
                tvUserInitial.text = "U"
            }

            if (subscriber.checkedIn) {
                tvCheckInStatus.text = "Presente"
                tvCheckInStatus.setBackgroundResource(R.drawable.bg_badge_primary)
                btnToggleCheckIn.text = "Desfazer"
                btnToggleCheckIn.setTextColor(ContextCompat.getColor(itemView.context, R.color.gray_text))
            } else {
                tvCheckInStatus.text = "Pendente"
                tvCheckInStatus.setBackgroundResource(R.drawable.bg_badge_open)
                btnToggleCheckIn.text = "Check-in"
                btnToggleCheckIn.setTextColor(ContextCompat.getColor(itemView.context, R.color.primary_blue))
            }

            btnToggleCheckIn.setOnClickListener {
                onToggleCheckInClick(subscriber)
            }
        }
    }
}
